#include "llvm/Pass.h"
#include "llvm/Passes/PassBuilder.h"
#include "llvm/Passes/PassPlugin.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/Utils/BasicBlockUtils.h"
#include "llvm/FuzzMutate/IRMutator.h"
#include "llvm/Analysis/TargetLibraryInfo.h"
#include "llvm/Transforms/Scalar/DCE.h"

#include <unordered_map>
#include <queue>

using namespace llvm;

namespace {

struct SkeletonPass : public PassInfoMixin<SkeletonPass> {
    PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM) {
        for (auto &F : M) {
            std::queue<Instruction*> deadInsts;
            // known values
            std::unordered_map<Value*, Value*> ptr2Op;
            std::unordered_map<Value*, Value*> op2Op;
            for (auto &B: F) {
                for (auto &I : B) {
                    if (auto* op = dyn_cast<StoreInst>(&I)) {
                        Value *ptr = op->getPointerOperand();
                        Value *val = op->getValueOperand();

                        ptr2Op[ptr] = val;

                    } else if (auto* op = dyn_cast<LoadInst>(&I)) {
                        Value *ptr = op->getPointerOperand();
                        if (ptr2Op.find(ptr) != ptr2Op.end()) {
                            // if there is a "canonical home" for the value stored @ ptr
                            op2Op[op] = ptr2Op[ptr];
                            deadInsts.push(op);
                        } else {
                            ptr2Op[ptr] = op;
                        }
                    } else if (auto* op = dyn_cast<BinaryOperator>(&I)) {
                        IRBuilder<> builder(op);

                        Value *lhs = op->getOperand(0);
                        if (op2Op.find(lhs) != op2Op.end()) {
                            op->setOperand(0, op2Op[lhs]);
                        }

                        Value *rhs = op->getOperand(1);
                        if (op2Op.find(rhs) != op2Op.end()) {
                             op->setOperand(1, op2Op[rhs]);
                        }
                    } 
    
                }
            }
            errs() << F << "\n\n";

            while (!deadInsts.empty()) {
                
                Instruction* inst = deadInsts.front(); deadInsts.pop();
                errs() << "dead code "<< *inst<<"\n"; 
            }
            errs() << "\n";

        }
        
        return PreservedAnalyses::none();
    };
};

}

extern "C" LLVM_ATTRIBUTE_WEAK ::llvm::PassPluginLibraryInfo
llvmGetPassPluginInfo() {
    return {
        .APIVersion = LLVM_PLUGIN_API_VERSION,
        .PluginName = "Skeleton pass",
        .PluginVersion = "v0.1",
        .RegisterPassBuilderCallbacks = [](PassBuilder &PB) {
            PB.registerPipelineStartEPCallback(
                [](ModulePassManager &MPM, OptimizationLevel Level) {
                    MPM.addPass(SkeletonPass());
                });
        }
    };
}
