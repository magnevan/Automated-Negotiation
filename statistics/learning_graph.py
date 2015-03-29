from utility import Utility
import json
from collections import defaultdict
import math
import pdb
import matplotlib.pyplot as plt

logfile = "OpponentModel-Session_20150329-154140.json"
op_utility = {
    "Party 2" : Utility("party2_utility.xml"),
    "Party 3" : Utility("party3_utility.xml")
}

datas = defaultdict(list)
with open(logfile) as log:
    for line in log:
        try :
            models = json.loads(line)
        except:
            print "failed to read line"
            continue
        for opponent in op_utility:
            diff = []
            model = models[opponent]
            for issue in model["weights"]:
                # weight of issue
                diff.append(
                    model["weights"][issue]
                    - op_utility[opponent].weights[issue]
                )
                
                for item in model[issue]:
                    diff.append(
                        model[issue][item]
                        - op_utility[opponent].issues[issue][item]
                    )
            # get length of n dimensional vector
            length = math.sqrt(sum(v*v for v in diff))
            datas[opponent].append(length)

for opponent, data in datas.iteritems():
    plt.plot(data)
    plt.show()
    
        
        
