package data_sturcts;

import ArrivalBuffer;
import Parcel;
import java.util.logging.*;


public class DestinationSorter {
    private static final Logger logger = Logger.getLogger(DestinationSorter.class.getName());
    
    private class Node {
        String cityName;
        ArrivalBuffer parcelList;
        Node left, right;

        Node(String cityName) {
            this.cityName = cityName;
            this.parcelList = new ArrivalBuffer(Integer.MAX_VALUE); // max as def cap value::
            this.left = null;
            this.right = null;
        }
    }

    private Node root;
    //variarble for logging::
    private int totalParcelsSorted;
    private int totalParcelsDispatched;
    private int failedOperations;
    private int recoveryAttempts;
    
    public DestinationSorter() {
        root = null;
        totalParcelsSorted = 0;
        totalParcelsDispatched = 0;
        failedOperations = 0;
        recoveryAttempts = 0;
    }

    // 🟢 Parcel ekleme
    public void insertParcel(Parcel parcel) {
        //ERror handling::

        try {
            if (parcel == null) {
                throw new IllegalArgumentException("Parcel cannot be null");
            }
            if (parcel.getDestinationCity() == null || parcel.getDestinationCity().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid destination city");
            }
            if (parcel.getParcelID() == null || parcel.getParcelID().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid parcel ID");
            }

            // Validate system state
            validateSystemState();

            root = insertParcelRecursive(root, parcel);
            totalParcelsSorted++;
            logger.info(String.format("[Sort] Parcel %s sorted to %s (Priority: %d)", 
                parcel.getParcelID(), parcel.getDestinationCity(), parcel.getPriority()));
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to insert parcel: %s", e.getMessage()));
            handleError(e);
        }
    }

    private Node insertParcelRecursive(Node node, Parcel parcel) {
        if (node == null) {
            Node newNode = new Node(parcel.getDestinationCity());
            try {
                newNode.parcelList.enqueue(parcel);
                logger.info(String.format("[New City] Created node for %s", parcel.getDestinationCity()));
            } catch (Exception e) {
                logger.severe(String.format("[Error] Queue overflow for city %s", parcel.getDestinationCity()));
                handleError(e);
            }
            return newNode;
        }

        int compare = parcel.getDestinationCity().compareToIgnoreCase(node.cityName);
        if (compare < 0) {
            node.left = insertParcelRecursive(node.left, parcel);
        } else if (compare > 0) {
            node.right = insertParcelRecursive(node.right, parcel);
        } else {
            // Aynı şehirse kuyruğa ekle
            try {
                node.parcelList.enqueue(parcel);
            } catch (Exception e) {
                logger.severe(String.format("[Error] Queue overflow for city %s", node.cityName));
                handleError(e);
            }
        }
        return node;
    }

    // 🟡 Belirli bir şehir için kuyruktaki tüm kargoları al
    public ArrivalBuffer getCityParcels(String city) {
        try {
            // Validate input
            if (city == null || city.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid city name");
            }

            Node node = search(root, city);
            if (node != null) {
                logger.info(String.format("[City Status] %s has %d parcels in queue", 
                    city, node.parcelList.size()));
            }
            return (node != null) ? node.parcelList : null;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to get city parcels: %s", e.getMessage()));
            handleError(e);
            return null;
        }
    }

    private Node search(Node node, String city) {
        if (node == null) return null;
        int compare = city.compareToIgnoreCase(node.cityName);
        if (compare < 0) return search(node.left, city);
        else if (compare > 0) return search(node.right, city);
        else return node;
    }

    // 🔴 Belirli şehirden bir parcel sil (kargo gönderildikten sonra)
    public boolean removeParcel(String city, String parcelID) {
        try {
            // Validate input
            if (city == null || city.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid city name");
            }
            if (parcelID == null || parcelID.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid parcel ID");
            }

            Node node = search(root, city);
            if (node != null && !node.parcelList.isEmpty()) {
                // Create a temporary buffer to hold parcels
                ArrivalBuffer tempBuffer = new ArrivalBuffer(Integer.MAX_VALUE);
                boolean found = false;
                
                // Move all parcels except the one to remove to temp buffer
                while (!node.parcelList.isEmpty()) {
                    Parcel p = node.parcelList.dequeue();
                    if (!p.getParcelID().equals(parcelID)) {
                        tempBuffer.enqueue(p);
                    } else {
                        found = true;
                        totalParcelsDispatched++;
                        logger.info(String.format("[Dispatch] Parcel %s dispatched from %s", 
                            parcelID, city));
                    }
                }
                
                // Move parcels back to original buffer
                while (!tempBuffer.isEmpty()) {
                    node.parcelList.enqueue(tempBuffer.dequeue());
                }
                
                return found;
            }
            return false;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to remove parcel: %s", e.getMessage()));
            handleError(e);
            return false;
        }
    }

    // 🟢 Şehir adına göre alfabetik sıralı BST dolaşımı
    public void inOrderTraversal() {
        try {
            logger.info("\n===+ Current BST Status +===");
            inOrderRecursive(root);
            logger.info("===+ End BST Status +===\n");
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to traverse BST: %s", e.getMessage()));
            handleError(e);
        }
    }

    private void inOrderRecursive(Node node) {
        if (node != null) {
            inOrderRecursive(node.left);
            logger.info(String.format("City: %s | Parcel Count: %d", 
                node.cityName, node.parcelList.size()));
            inOrderRecursive(node.right);
        }
    }

    // 🔍 Şehirde kaç kargo var?
    public int countCityParcels(String city) {
        try {
            // Validate input
            if (city == null || city.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid city name");
            }

            Node node = search(root, city);
            int count = (node != null) ? node.parcelList.size() : 0;
            logger.info(String.format("[City Count] %s has %d parcels", city, count));
            return count;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to count city parcels: %s", e.getMessage()));
            handleError(e);
            return 0;
        }
    }

    // 🌳 BST yüksekliği
    public int getHeight() {
        try {
            int height = calculateHeight(root);
            logger.info(String.format("[BST Height] Current height: %d", height));
            return height;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to get BST height: %s", e.getMessage()));
            handleError(e);
            return 0;
        }
    }

    private int calculateHeight(Node node) {
        if (node == null) return 0;
        return 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
    }

    // 📊 Toplam şehir (düğüm) sayısı
    public int getCityCount() {
        try {
            int count = countNodes(root);
            logger.info(String.format("[City Count] Total cities in BST: %d", count));
            return count;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to get city count: %s", e.getMessage()));
            handleError(e);
            return 0;
        }
    }

    private int countNodes(Node node) {
        if (node == null) return 0;
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    // 🚩 En çok yüke sahip şehir (en fazla parcel içeren node)
    public String getBusiestCity() {
        try {
            String busiest = findMaxCity(root, null, 0);
            if (busiest != null) {
                Node node = search(root, busiest);
                logger.info(String.format("\n[Busiest City] %s with %d parcels", 
                    busiest, node.parcelList.size()));
            }
            return busiest;
        } catch (Exception e) {
            failedOperations++;
            logger.severe(String.format("[Error] Failed to get busiest city: %s", e.getMessage()));
            handleError(e);
            return null;
        }
    }

    private String findMaxCity(Node node, String maxCity, int maxCount) {
        if (node == null) return maxCity;
        if (node.parcelList.size() > maxCount) {
            maxCity = node.cityName;
            maxCount = node.parcelList.size();
        }
        maxCity = findMaxCity(node.left, maxCity, maxCount);
        maxCity = findMaxCity(node.right, maxCity, maxCount);
        return maxCity;
    }

    // Error handling methods
    private void validateSystemState() {
        if (root == null && totalParcelsSorted > 0) {
            throw new IllegalStateException("System state inconsistency: parcels exist but BST is empty");
        }
        if (totalParcelsDispatched > totalParcelsSorted) {
            throw new IllegalStateException("System state inconsistency: more parcels dispatched than sorted");
        }
    }

    private void handleError(Exception e) {
        recoveryAttempts++;
        logger.warning(String.format("[Recovery] Attempt %d: %s", recoveryAttempts, e.getMessage()));
        
        // Basic recovery: validate and fix system state
        try {
            validateSystemState();
        } catch (IllegalStateException ise) {
            logger.severe(String.format("[Recovery Failed] %s", ise.getMessage()));
        }
    }

    // Get system statistics
    public String getSystemStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("\n===+ System Statistics +===\n");
        stats.append(String.format("Total Parcels Sorted: %d\n", totalParcelsSorted));
        stats.append(String.format("Total Parcels Dispatched: %d\n", totalParcelsDispatched));
        stats.append(String.format("Failed Operations: %d\n", failedOperations));
        stats.append(String.format("Recovery Attempts: %d\n", recoveryAttempts));
        stats.append(String.format("BST Height: %d\n", getHeight()));
        stats.append(String.format("Total Cities: %d\n", getCityCount()));
        stats.append("===+ End Statistics +===\n");
        return stats.toString();
    }
}

