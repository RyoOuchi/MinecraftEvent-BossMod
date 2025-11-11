package com.example.examplemod.Networking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.*;

public class Graph {
    private final Map<BlockPos, Map<BlockPos, Integer>> adjacencyList = new HashMap<>();

    public void addRouter(BlockPos routerPos) {
        adjacencyList.putIfAbsent(routerPos, new HashMap<>());
    }

    public void addConnection(BlockPos fromRouter, BlockPos toRouter, int cost) {
        adjacencyList.putIfAbsent(fromRouter, new HashMap<>());
        adjacencyList.putIfAbsent(toRouter, new HashMap<>());
        adjacencyList.get(fromRouter).put(toRouter, cost);
        adjacencyList.get(toRouter).put(fromRouter, cost); // undirected connection
    }

    public Map<BlockPos, Integer> getConnections(BlockPos routerPos) {
        return adjacencyList.getOrDefault(routerPos, new HashMap<>());
    }

    public Set<BlockPos> getRouters() {
        return adjacencyList.keySet();
    }

    // Dijkstra's algorithm to find the shortest path between two routers
    public List<BlockPos> findShortestPath(BlockPos start, BlockPos end) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        PriorityQueue<Map.Entry<BlockPos, Integer>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos router : adjacencyList.keySet()) {
            distances.put(router, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(Map.entry(start, 0));

        while (!pq.isEmpty()) {
            BlockPos current = pq.poll().getKey();
            if (!visited.add(current)) continue;
            if (current.equals(end)) break;

            for (Map.Entry<BlockPos, Integer> neighbor : getConnections(current).entrySet()) {
                BlockPos next = neighbor.getKey();
                int edgeCost = neighbor.getValue();
                int newDist = distances.get(current) + edgeCost;

                if (newDist < distances.get(next)) {
                    distances.put(next, newDist);
                    previous.put(next, current);
                    pq.add(Map.entry(next, newDist));
                }
            }
        }

        List<BlockPos> path = new ArrayList<>();
        for (BlockPos at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        // Return empty list if no valid path
        if (path.isEmpty() || !path.get(0).equals(start)) {
            return Collections.emptyList();
        }

        return path;
    }

    public void visualizeNetwork(Level level, Block markerBlock) {
        if (level == null || level.isClientSide()) return;

        System.out.println("🗺️ Debugging network layout: " + adjacencyList.size() + " routers.");
        for (BlockPos routerPos : adjacencyList.keySet()) {
            // place a marker 2 blocks above each router position
            BlockPos markerPos = routerPos.above(2);

            // only place if the space is air / replaceable
            if (level.isEmptyBlock(markerPos)) {
                level.setBlock(markerPos, markerBlock.defaultBlockState(), 3);
                System.out.println("💡 Marker placed at " + markerPos + " (above router " + routerPos + ")");
            }
        }

        System.out.println("✅ Finished visualizing router graph.");
    }

    @Nullable
    public BlockPos findClosestTarget(BlockPos start, List<BlockPos> targets) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        PriorityQueue<Map.Entry<BlockPos, Integer>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos router : adjacencyList.keySet()) {
            distances.put(router, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(Map.entry(start, 0));

        BlockPos closestTarget = null;
        int minDistance = Integer.MAX_VALUE;

        while (!pq.isEmpty()) {
            Map.Entry<BlockPos, Integer> entry = pq.poll();
            BlockPos current = entry.getKey();
            int currentDist = entry.getValue();

            if (!visited.add(current)) continue;

            // if this router is a target, check if it's the closest
            if (targets.contains(current) && currentDist < minDistance) {
                closestTarget = current;
                minDistance = currentDist;
                break; // optional: stop when first found (since Dijkstra guarantees minimal)
            }

            for (Map.Entry<BlockPos, Integer> neighbor : getConnections(current).entrySet()) {
                BlockPos next = neighbor.getKey();
                int edgeCost = neighbor.getValue();
                int newDist = currentDist + edgeCost;

                if (newDist < distances.get(next)) {
                    distances.put(next, newDist);
                    pq.add(Map.entry(next, newDist));
                }
            }
        }

        return closestTarget; // may return null if no path exists
    }
}
