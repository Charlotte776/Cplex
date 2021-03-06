#END IN JFK IS NOT NECESSARY
# S_DU==-1 AND E_DU==-1  ELSE : ALL DU=i
#按照字母顺序遍历每一条路，这条路的路径放在trip中，最后一个点存放在endtrip中
#当第一条路走完后如果还有多余的点，则对trip进行pop，pop的点存入endtrip中，直到能把剩余的点接到trip的后面
#如果lenth==0&&trip[-1]==endtrip[0]路径能够接上，则结束循环


class Solution:

    def findItinerary(self, tickets):
        """
        :type tickets: List[List[str]]
        :rtype: List[str]
        """
        #add point to trip or endtrip
        trip = ['A']    #JFK
        endtrip = []
        #cou=0
        #if trip link endtrip: link ==1
        link = 0

        maxplane = tickets[0][1]
        for j in range(0,len(tickets)):
                if tickets[j][1]>maxplane:
                    maxplane=tickets[j][1]
            
        while 1 :
            if len(tickets) == 0:
                break
            #du
            endm = -1  
            start = trip[-1]
            smllet=maxplane
            
            for i in range(0,len(tickets)):
                if tickets[i][0] == start:
                    if len(endtrip) > 0:
                        if tickets[i][1] == endtrip[0]:
                            link = 1
                            continue
                    #hoose smaller letter if have
                    if smllet >= tickets[i][1]:
                        smllet = tickets[i][1]
                        endm = i
            #find point du==-1            
            if endm == -1:
                if link == 1:
                    break
                endtrip.append(trip.pop())
                continue
            #find point du!=-1
            trip.append(smllet)
            #cou+=1
            del tickets[endm]   
        trip+=endtrip[::-1]
        return trip
    
#test
tickets= [["A","Z"],["Z","A"],["A","D"],["D","A"],["D","F"],["F","D"]]
p=Solution
lst=p.findItinerary(p,tickets)
print('\n')
print(lst)
