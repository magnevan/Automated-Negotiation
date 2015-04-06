from utility import *
import json
from collections import defaultdict
import math
import pdb
import matplotlib.pyplot as plt

logfile = "OpponentModel-Session_20150404-150205"
op_utility = {
    "Boulware" : Utility("party3_utility.xml"),
    "Conceder" : Utility("party2_utility.xml")
}

def read_json(op_utility):
    runfile = files_of_type('*.json')[0]
    datas = defaultdict(list)
    with open(runfile) as log:
        for line in log:
            try :
                models = json.loads(line)
            except:
                print "failed to read line"
                continue
            for opponent in op_utility:
                if not opponent in models:
                    pass
                
                diff = []
                model = models[opponent]
                for issue in model["weights"]:
                    # weight of issue
                    """                    
                    diff.append(
                        model["weights"][issue]
                        - op_utility[opponent].weights[issue]
                    )
                    """
                    for item in model[issue]:
                        diff.append(
                            model[issue][item] * model["weights"][issue]
                            - op_utility[opponent].issues[issue][item] * op_utility[opponent].weights[issue]
                        )
                    
                # get length of n dimensional vector
                length = math.sqrt(sum(v*v for v in diff))
                datas[opponent].append(length)
    return datas

# normalize data such that max is 1.0
"""
for data in datas.values():
    m = max(data)
    for i in xrange(len(data)):
        data[i] = data[i] / m
"""

datas = read_json(op_utility)
for opponent, data in datas.iteritems():
    plt.plot(data)
    plt.ylabel('error')
    plt.xlabel('time')
    plt.title(opponent)
    plt.show()
    
        
        
