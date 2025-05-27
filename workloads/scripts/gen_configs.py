FNS = ['0', '1', '2']
KF = {'1': '0','3': '1', '5': '2'}
RS = {'1': 150, '5': 300, '10': 400, '15': 500, '20': 600}
EPOCH_LEN = 60000 # 30000
SHAPE = "grid"
DIMS = "4,1"
TOPO = SHAPE + DIMS
VIRTUAL = {"1,1": 3, "2,2": 3, "4,4": 3, "8,8": 3, "12,12": 3}
POS = ['outer']
EXP_NO = 10

for k, f in KF.items():
    for r, exp_len in RS.items():
        for fn in FNS:
            for v, n in VIRTUAL.items():
                for p in POS:
                    for e in range(EXP_NO):
                        fn_chunk = f'{TOPO}_v{v}_n{n}_{p}'
                        config_fn = f'configs/config_{fn_chunk}_r{r}_f{fn}_k{k}_e{e}.json'
                        config = '{\n' + \
                                '    "debug": "false",\n' + \
                                f'    "devTopoDims": "{DIMS}",\n' + \
                                f'    "devLocsFn": "workloads/dev_loc/dev_loc_{fn_chunk}.txt",\n' + \
                                f'    "devTopoFn": "workloads/dev_topo/dev_topo_{fn_chunk}.txt",\n' + \
                                f'    "devSchedFn": "workloads/dev_sched/dev_sched_{fn_chunk}.txt",\n' + \
                                '    "devClstrPlc": "LOCALITY",\n' + \
                                f'    "devClstrsFn": "workloads/dev_clstr/dev_clstr_{fn_chunk}.txt",\n' + \
                                '    "devKGrpRng": 4,\n' + \
                                '    "devMntrPeriod": 1000,\n' + \
                                '    "medleyOut": "../medley/outputMedley.txt",\n' + \
                                '    "waitCoef": 2000,\n' + \
                                f'    "nodesListFn": "workloads/node_list/node_list_{fn_chunk}.txt",\n' + \
                                f'    "nodeSchedFn": "workloads/node_sched/node_sched_{fn_chunk}_cp0.0.txt",\n' + \
                                f'    "rtnNo": {r},\n' + \
                                f'    "rtnsFn": "workloads/routines/{fn_chunk}/rtns_r{r}.txt",\n' + \
                                '    "rtnMntrPeriod": 1000,\n' + \
                                '    "rtnKGrpRng": 1,\n' + \
                                '    "K": ' + k + ',\n' + \
                                '    "F": ' + f + ',\n' + \
                                '    "minTriggerOffset": 10000,\n' + \
                                '    "kGrpSlctPlc": "LSHMIX",\n' + \
                                '    "lshParams": {\n' + \
                                '        "k": 2,\n' + \
                                '        "l": 2,\n' + \
                                '        "r": 4,\n' + \
                                '        "mean": 0.0,\n' + \
                                '        "std": 1.0,\n' + \
                                '        "rand": 0.1\n' + \
                                '    },\n' + \
                                '    "ldrElctnPlc": "LSH_SMALLEST_HASH",\n' + \
                                '    "lockStrategy": "SERIAL",\n' + \
                                f'    "epochLen": {exp_len*1000+20},\n' + \
                                '    "initDelay": 10000,\n' + \
                                '    "membershipMntrPeriod": 1000,\n' + \
                                '    "testType": "INKGROUP_BENCHMARK_WO_FAILURE",\n' + \
                                f'    "outDir": "outputs/{fn_chunk}_r{r}_f{fn}_k{k}_e{e}/",\n' + \
                                f'    "expLength": {exp_len*1000},\n' + \
                                '    "loadMntrPeriod": 1000,\n' + \
                                f'    "seed": {e}\n' + \
                                '}\n'
                        with open(config_fn, 'w+', encoding='utf-8') as file:
                            file.write(config)
                        print("Config saved in", config_fn)
