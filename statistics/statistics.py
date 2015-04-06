import re
import pdb
import sys
import matplotlib.pyplot as plt
import numpy as np
from utility import *
import json


ut = [
    Utility('party1_utility.xml'),
    Utility('party2_utility.xml'),
    Utility('party3_utility.xml')
]
"""
ut = [
    Utility('University_util7.xml'),
    Utility('University_util8.xml'),
    Utility('University_util9.xml')
]
"""

    

def read_csv():
    runfile = files_of_type('*.csv')[0]
    rounds = []
    with open(runfile) as run:
        run.next() # starting negotiation session
        line = run.next().strip()
        while line.startswith('Round') or line.startswith('Turn'):
            if line.startswith('Round'):
                r = int(line.split()[1]) - 1 # 0 indexed
                rounds.append([])
                
            elif line.startswith('Turn'):
                turn = {}
                party_name = re.match('Turn \d+: ([\w\d ]+)', line).groups()[0].strip()
                turn['party'] = party_name
                
                if line.find('Offer:') != -1:
                    offer_text = re.match('.*Offer: Bid\[(.*)\]', line).groups()[0][:-2]
                    offer = {}
                    turn['offer'] = offer
                    turn['accept'] = False
                    # This following regex is a bit convoluted, and I really didn't want to
                    # include it. However the problem is that the syntax of offer text is
                    # far from clean. As an example, it might be:
                    # 'Food: Chips and Nuts, Drinks: Beer Only, Location: Party Room,
                    # Invitations: Custom, Handmade, Music: DJ, Cleanup: Specialized Materials'
                    #
                    # As you might see, this is problematic because there's no clean delimiter for
                    # the list since comma can be a part of the item text (Custom, Handmade)
                    # This regex essentially tries to find the pattern: "words that end with a
                    # colon, followed by a list of words, optionally terminated by a comma.
                    # as long as the word doesn't end with a colon. Also, don't match the word
                    # that ends with a colon.
                    # Look, regex is hard ok? Just skip this line, it will only drive you mad.
                    #for issue_item in offer_text.split(', '):
                    for issue_item in re.findall('(\w+:(?: [\w,-]+(?! \w+:))+)', offer_text):
                        issue, item = issue_item.split(': ')
                        offer[issue] = item
                elif line.find('(Accept)') != -1:
                    turn['accept'] = True
                else:
                    print line

                rounds[r].append(turn)

            line = run.next().strip()

        # get the final data here if you want
    return rounds

def read_json():
    runfile = files_of_type('*.json')[0]
    with open(runfile) as log:
        data = map(json.loads, log.readlines())
    return data

def eval_utility(bid, ut):
    utility = 0.0
    for issue in bid:
        if not 'weights' in ut:
            # first round, we don't have models
            return 0.0
        utility += ut['weights'][issue] * ut[issue][bid[issue]]
    return utility

run_data = read_csv()
predicted_models = read_json()

plt.figure(1)
plt.subplot(111)
plt.title("Utilities from %s offers" % run_data[0][0]['party'])
plt.xlabel("Turn")
plt.ylabel("Utility")

for i in xrange(3):
    utility = []
    predicted_utility = [0.0]
    round_num = []
    party_name = run_data[0][i]['party']
    accepts = []
    for rn, r in enumerate(run_data):
        if not r[0]['accept']:
            utility.append(ut[i].get_utility(r[0]['offer']))
            round_num.append(rn)
            if rn < len(predicted_models) and party_name in predicted_models[rn]:
                predicted_utility.append(eval_utility(r[0]['offer'], predicted_models[rn][party_name]))
        if i < len(r) and r[i]['accept']:
            accepts.append((rn, utility[-1]))

    round_num, utility, predicted_utility \
        = map(np.array, [round_num, utility, predicted_utility])
    if len(predicted_utility) > 1:
        predicted_utility[0] = predicted_utility[1] # temp becayse missing data

    line = plt.plot(round_num, utility, linewidth=3, label=party_name)[0]
    if accepts:
        x, y = zip(*accepts)
        plt.plot(x, y, 'bs')
    if len(predicted_utility) == len(utility):
        plt.fill_between(
            round_num, utility, predicted_utility,
            facecolor=line._color,
            edgecolor=line._color,
            alpha=0.5
        )
        
        
        

plt.legend(loc=0)
plt.show()

    
