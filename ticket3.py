from collections import defaultdict
#ALL POINT ADD TO ENDTRIP
class Solution:

    def findItinerary(self, tickets):
        """
        :type tickets: List[List[str]]
        :rtype: List[str]
        """

        paixu = defaultdict(list)
        for k, v in tickets:
            paixu[k].append(v)
            #print(paixu[k])
        for k, v in paixu.items():
            v.sort(reverse=True)
        endtrip = []
        self.dfs(self, paixu, "A", endtrip)
        return endtrip[::-1]

    def dfs(self, paixu, start, endtrip):
        while paixu[start]:
            a = paixu[start].pop()
            #print(a)
            self.dfs(self,paixu, a, endtrip)
            #print(endtrip)
        #add from leaf point
        endtrip.append(start)

#test
tickets= [["A","Z"],["Z","A"],["A","D"],["D","A"],["D","F"],["F","D"]]
p=Solution
lst=p.findItinerary(p,tickets)
print('\n')
print(lst)


