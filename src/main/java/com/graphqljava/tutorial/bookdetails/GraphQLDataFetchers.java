package com.graphqljava.tutorial.bookdetails;

import com.google.common.collect.ImmutableMap;
import fr.uga.pddl4j.heuristics.relaxation.Heuristic;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;
import fr.uga.pddl4j.util.Plan;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GraphQLDataFetchers {

    private static List<ImmutableMap<String, String>> books = Arrays.asList(
            ImmutableMap.of("id", "book-1",
                    "name", "Harry Potter and the Philosopher's Stone",
                    "pageCount", "223",
                    "authorId", "author-1"),
            ImmutableMap.of("id", "book-2",
                    "name", "Moby Dick",
                    "pageCount", "635",
                    "authorId", "author-2"),
            ImmutableMap.of("id", "book-3",
                    "name", "Interview with the vampire",
                    "pageCount", "371",
                    "authorId", "author-3")
    );

    private static List<Map<String, String>> authors = Arrays.asList(
            ImmutableMap.of("id", "author-1",
                    "firstName", "Joanne",
                    "lastName", "Rowling"),
            ImmutableMap.of("id", "author-2",
                    "firstName", "Herman",
                    "lastName", "Melville"),
            ImmutableMap.of("id", "author-3",
                    "firstName", "Anne",
                    "lastName", "Rice")
    );

    public DataFetcher getBookByIdDataFetcher() {
        return dataFetchingEnvironment -> {
            String bookId = dataFetchingEnvironment.getArgument("id");
            return books
                    .stream()
                    .filter(book -> book.get("id").equals(bookId))
                    .findFirst()
                    .orElse(null);
        };
    }

    public DataFetcher getBooksDataFetcher() {
        return dataFetchingEnvironment -> {
            return books;
        };
    }

    public DataFetcher planFetcher() {
        return dataFetchingEnvironment -> {
            String domain = dataFetchingEnvironment.getArgument("domain");
            String problem = dataFetchingEnvironment.getArgument("problem");

            String plan = StateSpacePlannerFactory.plan(Planner.Name.HSP, domain, problem, 1, 600000, Heuristic.Type.FAST_FORWARD, 1,true, true);
            if (plan != null) {
                return plan.split("\n");
            }
            return new String[] { "Mo plan found" };
        };
    }

    public DataFetcher getAuthorDataFetcher() {
        return dataFetchingEnvironment -> {
            Map<String,String> book = dataFetchingEnvironment.getSource();
            String authorId = book.get("authorId");
            return authors
                    .stream()
                    .filter(author -> author.get("id").equals(authorId))
                    .findFirst()
                    .orElse(null);
        };
    }
}

/**
 * PROBLEM
 *
 * {
 *   plan(domain:";; logistics domain Typed version.\n;;\n\n(define (domain logistics)\n  (:requirements :strips :typing :action-costs)\n  (:types\n    truck airplane - vehicle\n    package vehicle - physobj\n    airport location - place\n    city place physobj - object\n  )\n\n  (:predicates\n    (in-city ?loc - place ?city - city)\n    (at ?obj - physobj ?loc - place)\n    (in ?pkg - package ?veh - vehicle)\n  )\n\n  (:functions\n    (total-cost)\n    (distance ?wp1 - place ?wp2 - place)\n  )\n\n  (:action LOAD-TRUCK\n    :parameters (?pkg - package ?truck - truck ?loc - place)\n    :precondition (and (at ?truck ?loc) (at ?pkg ?loc))\n    :effect (and\n      (not (at ?pkg ?loc))\n      (in ?pkg ?truck)\n      (increase (total-cost) 2)\n    )\n  )\n\n  (:action LOAD-AIRPLANE\n    :parameters (?pkg - package ?airplane - airplane ?loc - place)\n    :precondition (and (at ?pkg ?loc) (at ?airplane ?loc))\n    :effect (and\n      (not (at ?pkg ?loc))\n      (in ?pkg ?airplane)\n    )\n  )\n\n  (:action UNLOAD-TRUCK\n    :parameters (?pkg - package ?truck - truck ?loc - place)\n    :precondition (and (at ?truck ?loc) (in ?pkg ?truck))\n    :effect (and (not (in ?pkg ?truck)) (at ?pkg ?loc))\n  )\n\n  (:action UNLOAD-AIRPLANE\n    :parameters (?pkg - package ?airplane - airplane ?loc - place)\n    :precondition (and (in ?pkg ?airplane) (at ?airplane ?loc))\n    :effect (and (not (in ?pkg ?airplane)) (at ?pkg ?loc))\n  )\n\n  (:action DRIVE-TRUCK\n    :parameters (?truck - truck ?loc-from - place ?loc-to - place ?city - city)\n    :precondition (and (at ?truck ?loc-from) (in-city ?loc-from ?city) (in-city ?loc-to ?city))\n    :effect (and (not (at ?truck ?loc-from)) (at ?truck ?loc-to))\n  )\n\n  (:action FLY-AIRPLANE\n    :parameters (?airplane - airplane ?loc-from - airport ?loc-to - airport)\n    :precondition (at ?airplane ?loc-from)\n    :effect (and\n      (not (at ?airplane ?loc-from))\n      (at ?airplane ?loc-to)\n      (increase\n        (total-cost)\n        (distance ?loc-from ?loc-to))\n    )\n  )\n)",
 *   problem:"(define (problem logistics-4-0)\n  (:domain logistics)\n  (:objects\n    A380 A381 - airplane\n    aA aB aC aD - airport\n    A1 A2 B1 B2 C1 C2 D1 D2 - location\n    A B C D - city\n    truA truB truC truD - truck\n    obj1 obj2 obj3 - package\n  )\n\n  (:init\n    (= (total-cost) 0)\n    (= (distance aA aB) 300)\n    (= (distance aB aA) 300)\n    (= (distance aA aC) 600)\n    (= (distance aC aA) 600)\n    (= (distance aA aD) 500)\n    (= (distance aD aA) 500)\n    (= (distance aB aC) 650)\n    (= (distance aC aB) 650)\n    (= (distance aB aD) 450)\n    (= (distance aD aB) 450)\n    (= (distance aC aD) 100)\n    (= (distance aD aC) 100)\n    (at A380 aB)\n    (at A381 aC)\n\n    (at truA A2)\n    (at truB B1)\n    (at truC C2)\n    (at truD D1)\n\n    (at obj1 B1)\n    (at obj2 B2)\n    (at obj3 C1)\n\n    (in-city aA A)\n    (in-city A1 A)\n    (in-city A2 A)\n    (in-city aB B)\n    (in-city B1 B)\n    (in-city B2 B)\n    (in-city aC C)\n    (in-city C1 C)\n    (in-city C2 C)\n    (in-city aD D)\n    (in-city D1 D)\n    (in-city D2 D)\n\n  )\n\n  (:goal\n    (and\n      (at obj1 aD)\n      (at obj2 aA)\n      (at obj3 aA))\n  )\n\n  (:metric minimize\n    (total-cost)\n  )\n)\n"
 *   )
 * }
 */