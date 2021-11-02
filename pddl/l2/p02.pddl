(define (problem logistics-4-0)
  (:domain logistics)
  (:objects
    A380 A381 - airplane
    aA aB aC aD - airport
    A1 A2 B1 B2 C1 C2 D1 D2 - location
    A B C D - city
    truA truB truC truD - truck
    obj1 obj2 obj3 - package
  )

  (:init
    (= (total-cost) 0)
    (= (distance aA aB) 300)
    (= (distance aB aA) 300)
    (= (distance aA aC) 600)
    (= (distance aC aA) 600)
    (= (distance aA aD) 500)
    (= (distance aD aA) 500)
    (= (distance aB aC) 650)
    (= (distance aC aB) 650)
    (= (distance aB aD) 450)
    (= (distance aD aB) 450)
    (= (distance aC aD) 100)
    (= (distance aD aC) 100)
    (at A380 aB)
    (at A381 aC)

    (at truA A2)
    (at truB B1)
    (at truC C2)
    (at truD D1)

    (at obj1 B1)
    (at obj2 B2)
    (at obj3 C1)

    (in-city aA A)
    (in-city A1 A)
    (in-city A2 A)
    (in-city aB B)
    (in-city B1 B)
    (in-city B2 B)
    (in-city aC C)
    (in-city C1 C)
    (in-city C2 C)
    (in-city aD D)
    (in-city D1 D)
    (in-city D2 D)

  )

  (:goal
    (and
      (at obj1 aD)
      (at obj2 aA)
      (at obj3 aA))
  )

  (:metric minimize
    (total-cost)
  )
)
