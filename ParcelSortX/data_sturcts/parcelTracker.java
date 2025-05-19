package data_sturcts;

import java.util.logging.*;

public class ParcelTracker {
    private static final Logger logger = Logger.getLogger(ParcelTracker.class.getName());
    
    // Constants for hash table
    private static final int INITIAL_CAPACITY = 30;  // Based on QUEUE_CAPACITY from config.txt
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;  // As specified in requirements
    
    // Parcel status enum
    public enum ParcelStatus {
        IN_QUEUE,
        SORTED,
        DISPATCHED,
        RETURNED
    }
    
    // Node class for parcel data
    private class ParcelNode {
        String parcelID;
        ParcelStatus status;
        int arrivalTick;
        int dispatchTick;
        int returnCount;
        String destinationCity;
        int priority;
        String size;
        ParcelNode next;  // For chaining
        
        ParcelNode(String parcelID, ParcelStatus status, int arrivalTick, 
                  String destinationCity, int priority, String size) {
            this.parcelID = parcelID;
            this.status = status;
            this.arrivalTick = arrivalTick;
            this.dispatchTick = -1;  // Not dispatched yet
            this.returnCount = 0;
            this.destinationCity = destinationCity;
            this.priority = priority;
            this.size = size;
            this.next = null;
        }
    }
    
    // Hash table structure
    private ParcelNode[] table;
    private int size;
    private int capacity;
    
    public ParcelTracker() {
        this.capacity = INITIAL_CAPACITY;
        this.table = new ParcelNode[capacity];
        this.size = 0;
        logger.info(String.format("[Initialize] ParcelTracker created with initial capacity %d (based on QUEUE_CAPACITY)", capacity));
    }
    
    // Hash function
    private int hash(String parcelID) {
        int hash = 0;
        for (char c : parcelID.toCharArray()) {
            hash = (hash * 31 + c) % capacity;
        }
        return Math.abs(hash);
    }
    
    // Insert a new parcel record
    public void insert(String parcelID, ParcelStatus status, int arrivalTick, 
                      String destinationCity, int priority, String size) {
        try {
            // Validate input
            if (parcelID == null || parcelID.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid parcel ID");
            }
            if (destinationCity == null || destinationCity.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid destination city");
            }
            if (priority < 1 || priority > 3) {
                throw new IllegalArgumentException("Invalid priority value");
            }
            if (size == null || !size.matches("Small|Medium|Large")) {
                throw new IllegalArgumentException("Invalid size value");
            }
            
            // Check if parcel already exists
            if (exists(parcelID)) {
                throw new IllegalStateException("Parcel already exists: " + parcelID);
            }
            
            // Check load factor and resize if necessary
            if ((double) size / capacity >= LOAD_FACTOR_THRESHOLD) {
                resize();
            }
            
            int index = hash(parcelID);
            ParcelNode newNode = new ParcelNode(parcelID, status, arrivalTick, 
                                              destinationCity, priority, size);
            
            // Insert at the beginning of the chain
            newNode.next = table[index];
            table[index] = newNode;
            size++;
            
            logger.info(String.format("[Insert] Parcel %s tracked with status %s", 
                parcelID, status));
                
        } catch (Exception e) {
            logger.severe(String.format("[Error] Failed to insert parcel %s: %s", 
                parcelID, e.getMessage()));
            throw e;
        }
    }
    
    // Update parcel status
    public void updateStatus(String parcelID, ParcelStatus newStatus) {
        try {
            ParcelNode node = getNode(parcelID);
            if (node == null) {
                throw new IllegalArgumentException("Parcel not found: " + parcelID);
            }
            
            ParcelStatus oldStatus = node.status;
            node.status = newStatus;
            
            // Update dispatch tick if parcel is being dispatched
            if (newStatus == ParcelStatus.DISPATCHED) {
                node.dispatchTick = getCurrentTick();  // You'll need to implement this
            }
            
            logger.info(String.format("[Status Update] Parcel %s: %s -> %s", 
                parcelID, oldStatus, newStatus));
                
        } catch (Exception e) {
            logger.severe(String.format("[Error] Failed to update status for parcel %s: %s", 
                parcelID, e.getMessage()));
            throw e;
        }
    }
    
    // Get parcel data
    public ParcelNode get(String parcelID) {
        try {
            ParcelNode node = getNode(parcelID);
            if (node == null) {
                throw new IllegalArgumentException("Parcel not found: " + parcelID);
            }
            return node;
        } catch (Exception e) {
            logger.severe(String.format("[Error] Failed to get parcel %s: %s", 
                parcelID, e.getMessage()));
            throw e;
        }
    }
    
    // Increment return count
    public void incrementReturnCount(String parcelID) {
        try {
            ParcelNode node = getNode(parcelID);
            if (node == null) {
                throw new IllegalArgumentException("Parcel not found: " + parcelID);
            }
            
            node.returnCount++;
            logger.info(String.format("[Return] Parcel %s return count: %d", 
                parcelID, node.returnCount));
                
        } catch (Exception e) {
            logger.severe(String.format("[Error] Failed to increment return count for parcel %s: %s", 
                parcelID, e.getMessage()));
            throw e;
        }
    }
    
    // Check if parcel exists
    public boolean exists(String parcelID) {
        return getNode(parcelID) != null;
    }
    
    // Helper method to get node
    private ParcelNode getNode(String parcelID) {
        int index = hash(parcelID);
        ParcelNode current = table[index];
        
        while (current != null) {
            if (current.parcelID.equals(parcelID)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }
    
    // Resize hash table
    private void resize() {
        int oldCapacity = capacity;
        capacity *= 2;
        ParcelNode[] oldTable = table;
        table = new ParcelNode[capacity];
        size = 0;
        
        // Rehash all entries
        for (int i = 0; i < oldCapacity; i++) {
            ParcelNode current = oldTable[i];
            while (current != null) {
                ParcelNode next = current.next;
                int newIndex = hash(current.parcelID);
                current.next = table[newIndex];
                table[newIndex] = current;
                current = next;
            }
        }
        
        logger.info(String.format("[Resize] Hash table resized to capacity %d", capacity));
    }
    
    // Get current simulation tick (to be implemented by simulation engine)
    private int getCurrentTick() {
        // This should be implemented to get the current tick from the simulation
        return 0;  // Placeholder
    }
    
    // Get statistics for reporting
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("\n===+ ParcelTracker Statistics +===\n");
        
        // Basic statistics
        stats.append(String.format("Total Parcels: %d\n", size));
        stats.append(String.format("Table Capacity: %d\n", capacity));
        stats.append(String.format("Load Factor: %.2f\n", (double) size / capacity));
        
        // Status counts and timing metrics
        int[] statusCounts = new int[ParcelStatus.values().length];
        int totalReturns = 0;
        int maxReturns = 0;
        String mostReturnedParcel = "None";
        int parcelsReturnedMoreThanOnce = 0;
        
        // Timing metrics
        long totalProcessingTime = 0;
        int processedParcels = 0;
        int maxDelay = 0;
        String longestDelayParcel = "None";
        
        // Traverse table to gather statistics
        for (ParcelNode node : table) {
            while (node != null) {
                // Status counts
                statusCounts[node.status.ordinal()]++;
                
                // Return statistics
                totalReturns += node.returnCount;
                if (node.returnCount > maxReturns) {
                    maxReturns = node.returnCount;
                    mostReturnedParcel = node.parcelID;
                }
                if (node.returnCount > 1) {
                    parcelsReturnedMoreThanOnce++;
                }
                
                // Processing time statistics
                if (node.status == ParcelStatus.DISPATCHED && node.dispatchTick != -1) {
                    int processingTime = node.dispatchTick - node.arrivalTick;
                    totalProcessingTime += processingTime;
                    processedParcels++;
                    
                    if (processingTime > maxDelay) {
                        maxDelay = processingTime;
                        longestDelayParcel = node.parcelID;
                    }
                }
                
                node = node.next;
            }
        }
        
        // Add status breakdown
        stats.append("\nStatus Breakdown:\n");
        for (ParcelStatus status : ParcelStatus.values()) {
            stats.append(String.format("  %s: %d\n", status, statusCounts[status.ordinal()]));
        }
        
        // Add parcels still in system
        int parcelsInSystem = statusCounts[ParcelStatus.IN_QUEUE.ordinal()] + 
                            statusCounts[ParcelStatus.SORTED.ordinal()];
        stats.append(String.format("\nParcels Still in System: %d\n", parcelsInSystem));
        
        // Add return statistics
        stats.append("\nReturn Statistics:\n");
        stats.append(String.format("  Total Returns: %d\n", totalReturns));
        stats.append(String.format("  Most Returns: %d (Parcel %s)\n", maxReturns, mostReturnedParcel));
        stats.append(String.format("  Parcels Returned More Than Once: %d\n", parcelsReturnedMoreThanOnce));
        
        // Add timing statistics
        stats.append("\nTiming Statistics:\n");
        if (processedParcels > 0) {
            double avgProcessingTime = (double) totalProcessingTime / processedParcels;
            stats.append(String.format("  Average Processing Time: %.2f ticks\n", avgProcessingTime));
            stats.append(String.format("  Longest Delay: %d ticks (Parcel %s)\n", maxDelay, longestDelayParcel));
        } else {
            stats.append("  No parcels have been processed yet\n");
        }
        
        stats.append("===+ End Statistics +===\n");
        return stats.toString();
    }
}
