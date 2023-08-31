import json
import sys
from collections import Counter


def count_op_types(prog):
    """ Counts and prints the frequencies of different types of instructions in the given Bril program.

    Args:
        prog (_type_): A Python JSON object representing a Bril program.
    """
    op_counts = Counter()
    for func in prog['functions']:
        for instr in func['instrs']:
            if "op" in instr:
                op_counts[instr['op']] += 1

    print(
        'Here are the types of instructions in this Bril program (exluding labels), sorted by frequency: \n'
    )
    for k, v in op_counts.most_common():
        print(k + ": " + str(v))


if __name__ == '__main__':
    prog = json.load(sys.stdin)
    count_op_types(prog)