import xml.etree.ElementTree as ET
import fnmatch
import os

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

def files_of_type(pattern):
    return fnmatch.filter(os.listdir('.'), pattern)
        
