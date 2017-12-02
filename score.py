import csv
from math import sqrt
from sys import argv

if len(argv) != 2:
    raise ValueError("len(argv) != 2")

lines = []
with open(argv[1], 'r') as csvfile:
    reader = csv.reader(csvfile, delimiter=";")
    for line in reader:
        lines.append(line)

distances = {}
utilities = {}
for line in lines:
    try:
        for i in range(3):
            agent_name = line[12 + i]
            agent_name = agent_name[:agent_name.rindex("@")]
            utility = float(line[15 + i])
            distance = float(line[10])
            utilities.setdefault(agent_name, []).append(utility)
            distances.setdefault(agent_name, []).append(distance)
    except ValueError:
        pass
    except IndexError:
        pass

pad = 20
for agent in utilities:
    avg_utility = sum(utilities[agent]) / len(utilities[agent])
    avg_dist = sum(distances[agent]) / len(distances[agent])
    print((agent + ":").ljust(pad), "dist2Nash =", str(avg_dist).ljust(pad), "utility =", str(avg_utility).ljust(pad))
