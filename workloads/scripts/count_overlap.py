import json
import numpy as np

fns = ['workloads/routines/rtn' + str(i) + '.json' for i in range(20)]
counts = []
for i, fn in enumerate(fns):
    with open(fn, 'r') as f:
        obj = json.load(f)
        curr = set()
        for o in obj['cmds']:
            idx = o['actDevID'].split('.')[-1]
            curr.add(int(idx))
        counts.append(curr)

exists = {}
for ct in counts:
    for idx in ct:
        exists[idx] = exists.get(idx, 0) + 1

result = []
for ct in counts:
    result.append((sum([exists[idx] for idx in ct]) - len(ct)) / len(ct))
print(np.mean(result))