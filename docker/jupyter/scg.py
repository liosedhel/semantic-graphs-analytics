import scg_pb2 as scg_pb
import os
import networkx as nx
import numpy as np
import matplotlib.pyplot as plt


def read_scg_file(file_name):
    if os.path.isfile(file_name) and file_name.endswith(".semanticgraphdb"):
        with open(file_name, "rb") as f:
            scg_file = scg_pb.SemanticGraphFile()
            scg_file.ParseFromString(f.read())
    return scg_file


def read_scg(workspace):
    scg_folder = os.path.join(workspace, ".semanticgraphs")
    scg = []
    for root, dir, files in os.walk(scg_folder):
        for f in files:
            scg.append(read_scg_file(os.path.join(root, f)))
    return scg


def create_graph(scgs):
    G = nx.DiGraph()
    for scg_file in scgs:
        for node in scg_file.nodes:
            G.add_node(node.id, scg_node=node)
            for edges in node.edges:
                G.add_edge(node.id, edges.to)
    for n in list(G.nodes()):
        scg_node = G.nodes[n].get("scg_node")
        # filter out not visible in the project nodes
        if not (G.degree(n) > 0 and scg_node is not None and scg_node.location.uri != "" and scg_node.kind != "FILE" and scg_node.kind != "PACKAGE_OBJECT"):
            G.remove_node(n)
    return G


def show_graph(G):
    # Compute the layout using the ForceAtlas2 algorithm
    pos = nx.layout.spring_layout(G)

    # Draw the graph using Matplotlib
    fig, ax = plt.subplots(figsize=(16, 9))
    nx.draw_networkx_edges(G, pos, node_size=5, alpha=0.1, ax=ax)
    nx.draw_networkx_nodes(G, pos, node_size=5, alpha=0.5, ax=ax)
    plt.show()


def show_graph_distribution(G):
    plt.clf()
    degree_sequence = sorted((d for n, d in G.degree()), reverse=True)[0:100]

    fig = plt.figure("Degree of a SCG", figsize=(8, 8))
    # Create a gridspec for adding subplots of different sizes
    axgrid = fig.add_gridspec(1, 4)

    ax1 = fig.add_subplot(axgrid[0:, :2])
    ax1.plot(degree_sequence, "b-", marker="o")
    ax1.set_title("Degree Rank Plot")
    ax1.set_ylabel("Degree")
    ax1.set_xlabel("Rank")

    ax2 = fig.add_subplot(axgrid[0:, 2:])
    ax2.bar(*np.unique(degree_sequence, return_counts=True))
    ax2.set_title("Degree histogram")
    ax2.set_xlabel("Degree")
    ax2.set_ylabel("# of Nodes")

    plt.show()


def main():
    # scgs = read_scg("/Users/kborowski/phd/metals")
    scgs = read_scg("/Users/kborowski/phd/phototrip-lagom")
    G = create_graph(scgs)
    show_graph(G)
    show_graph_distribution(nx.Graph(G))


if __name__ == "__main__":
    main()
