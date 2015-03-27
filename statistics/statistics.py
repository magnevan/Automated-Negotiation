import xml.etree.ElementTree as ET
import re
import pdb
import sys
import matplotlib.pyplot as plt

class Utility:
    def __init__(self, fname):
        obj = ET.parse(fname).getroot()[0]
        issues = obj.findall('issue')
        weights = obj.findall('weight')
        self.weights = {}
        for i in xrange(len(issues)):
            self.weights[issues[i].attrib['name']] = float(weights[i].attrib['value'])

        self.issues = {}
        for issue in issues:
            data = {
                item.attrib['value'] : float(item.attrib['evaluation']) \
                for item in issue.findall('item')
            }
            # normalize values
            item_max = max(data.values())
            for k, v in data.iteritems():
                data[k] = v / item_max

            self.issues[issue.attrib['name']] = data

    def print_weights(self):
        for issue, item in self.issues.iteritems():
            print issue, self.weights[issue]
            for k, v in item.iteritems():
                print '    ', v, k

    def get_utility(self, offer):
        utility = 0.0
        for issue, item in offer.iteritems():
            utility += self.weights[issue] * self.issues[issue][item]
        return utility

ut = [
    Utility('party1_utility.xml'),
    Utility('party2_utility.xml'),
    Utility('party3_utility.xml')
]


runfile = 'Log-Session_20150325-160410.csv'

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

data = [[], [], []]
for r in rounds:
    for i in xrange(3):
        if r[0]['accept']:
            data[i].append(0.0) # todo fix this incorrect
        else:
            data[i].append(ut[i].get_utility(r[0]['offer']))

for d in data:
    plt.plot(d)
plt.ylim(0, 1)
plt.xlim(0, len(data[0])-1)

plt.show()

    
