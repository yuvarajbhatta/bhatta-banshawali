#!/usr/bin/env python3
"""Generates a synthetic multi-generation family tree as SQL, for
performance benchmarking (docs/08 Phase 5 -- 500/2,000/10,000-person
load test gate). Prints INSERT statements to stdout; pipe into `mysql`
against a DISPOSABLE database only, never production:

    python3 generate-synthetic-family.py --count 2000 | \
        mysql -h 127.0.0.1 -P 3307 -u familytree_dev -pfamilytree_dev familytree_dev

Truncates persons/relationships first, so each run replaces the previous
one rather than compounding on top of it. Builds a plausible tree: one
root couple, then each couple has a handful of children, roughly 70% of
whom get paired with a synthetic "married-in" spouse and go on to have
their own children the next generation -- growth continues until the
target count is reached, so the actual generation depth is derived from
--count and --children-per-couple, not fixed.

Relationship rows are written in both directions (FATHER/MOTHER +
reciprocal CHILD, SPOUSE both ways), matching what
RelationshipService#saveRelationshipWithAutoLinks would create through
the app, so the generated data looks the same as if entered by hand.
"""
import argparse
import random
import sys

FIRST_NAMES_MALE = [
    "Ram", "Shyam", "Hari", "Krishna", "Gopal", "Bishnu", "Mohan", "Suresh",
    "Dinesh", "Prakash", "Rajesh", "Bikash", "Sanjay", "Deepak", "Ganesh",
    "Yubaraj", "Narayan", "Madhav", "Keshav", "Bhim",
]
FIRST_NAMES_FEMALE = [
    "Sita", "Gita", "Radha", "Laxmi", "Kamala", "Sarita", "Anita", "Sunita",
    "Rita", "Maya", "Sabitri", "Parbati", "Devi", "Sushma", "Kalpana",
    "Manisha", "Rekha", "Nirmala", "Pramila", "Sarada",
]
LAST_NAME = "Bhatta"
BASE_YEAR = 1850
YEARS_PER_GENERATION = 27


class Person:
    __slots__ = ("id", "generation", "first_name", "gender", "birth_year")

    def __init__(self, generation, gender, birth_year):
        self.id = None
        self.generation = generation
        self.gender = gender
        self.first_name = random.choice(FIRST_NAMES_MALE if gender == "M" else FIRST_NAMES_FEMALE)
        self.birth_year = birth_year


def build_tree(count, children_per_couple, spouse_probability, seed):
    random.seed(seed)
    people = []
    relationships = []  # (person, related_person, type) -- one row per direction already included

    def add_person(generation, gender):
        person = Person(generation, gender, BASE_YEAR + generation * YEARS_PER_GENERATION)
        people.append(person)
        return person

    def marry(a, b):
        relationships.append((a, b, "SPOUSE"))
        relationships.append((b, a, "SPOUSE"))

    def have_child(father, mother, generation):
        gender = random.choice(["M", "F"])
        child = add_person(generation, gender)
        relationships.append((child, father, "FATHER"))
        relationships.append((father, child, "CHILD"))
        relationships.append((child, mother, "MOTHER"))
        relationships.append((mother, child, "CHILD"))
        return child

    root_father = add_person(1, "M")
    root_mother = add_person(1, "F")
    marry(root_father, root_mother)

    current_couples = [(root_father, root_mother)]
    generation = 1

    while len(people) < count and current_couples:
        generation += 1
        next_couples = []
        for father, mother in current_couples:
            for _ in range(children_per_couple):
                if len(people) >= count:
                    break
                child = have_child(father, mother, generation)

                if len(people) < count and random.random() < spouse_probability:
                    spouse_gender = "F" if child.gender == "M" else "M"
                    spouse = add_person(generation, spouse_gender)
                    marry(child, spouse)
                    couple = (child, spouse) if child.gender == "M" else (spouse, child)
                    next_couples.append(couple)
        current_couples = next_couples

    for index, person in enumerate(people, start=1):
        person.id = index

    return people, relationships


def sql_escape(value):
    return value.replace("'", "''")


def emit_sql(people, relationships):
    print("SET FOREIGN_KEY_CHECKS=0;")
    print("TRUNCATE TABLE relationships;")
    print("TRUNCATE TABLE persons;")
    print("SET FOREIGN_KEY_CHECKS=1;")

    print(
        "INSERT INTO persons (id, generation_number, first_name, last_name, gender, birth_date) VALUES"
    )
    rows = []
    for person in people:
        birth_date = f"{person.birth_year:04d}-01-01"
        rows.append(
            f"({person.id}, {person.generation}, '{sql_escape(person.first_name)}', "
            f"'{LAST_NAME}', '{person.gender}', '{birth_date}')"
        )
    print(",\n".join(rows) + ";")

    print(
        "INSERT INTO relationships (person_id, related_person_id, relationship_type) VALUES"
    )
    rel_rows = [
        f"({person.id}, {related.id}, '{rel_type}')" for person, related, rel_type in relationships
    ]
    print(",\n".join(rel_rows) + ";")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--count", type=int, required=True, help="target number of people")
    parser.add_argument("--children-per-couple", type=int, default=3)
    parser.add_argument("--spouse-probability", type=float, default=0.7)
    parser.add_argument("--seed", type=int, default=42, help="fixed seed for reproducible runs")
    args = parser.parse_args()

    if args.count < 2:
        print("--count must be at least 2", file=sys.stderr)
        sys.exit(1)

    people, relationships = build_tree(args.count, args.children_per_couple, args.spouse_probability, args.seed)
    emit_sql(people, relationships)
    print(
        f"-- generated {len(people)} people across {max(p.generation for p in people)} generations, "
        f"{len(relationships)} relationship rows",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
