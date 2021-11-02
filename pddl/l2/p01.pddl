(define (problem logistics-4-0)
(:domain logistics)
(:objects
 A380 - airplane
 SydneyInternational BudapestFranzList - airport
 YowieBay City - location
 Budapest Sydney - city
 tru2 tru1 - truck
 obj23 obj22 obj21 obj13 obj12 obj11 - package)

(:init
 (= (distance SydneyInternational BudapestFranzList) 1000)
 (= (distance BudapestFranzList SydneyInternational) 1000)
 (at A380 BudapestFranzList)
 (at tru1 YowieBay) (at obj11 YowieBay)
 (at obj12 YowieBay) (at obj13 YowieBay) (at tru2 City) (at obj21 City) (at obj22 City)
 (at obj23 City) (in-city YowieBay Sydney) (in-city SydneyInternational Sydney) (in-city City Budapest)
 (in-city BudapestFranzList Budapest))

(:goal (and (at obj11 SydneyInternational) (at obj23 YowieBay) (at obj13 SydneyInternational) (at obj21 YowieBay)))

(:metric minimize (total-cost))
)
