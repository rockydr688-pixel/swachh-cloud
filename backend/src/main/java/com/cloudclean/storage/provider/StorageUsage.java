package com.cloudclean.storage.provider;

public class StorageUsage {

    private long totalCapacityBytes;
    private long usedBytes;
    private long availableBytes;
    private long trashBytes;

    public StorageUsage() {}

    public StorageUsage(long totalCapacityBytes, long usedBytes, long availableBytes) {
        this.totalCapacityBytes = totalCapacityBytes;
        this.usedBytes = usedBytes;
        this.availableBytes = availableBytes;
    }

    public StorageUsage(long totalCapacityBytes, long usedBytes, long availableBytes, long trashBytes) {
        this.totalCapacityBytes = totalCapacityBytes;
        this.usedBytes = usedBytes;
        this.availableBytes = availableBytes;
        this.trashBytes = trashBytes;
    }

    public long getTotalCapacityBytes() {
        return totalCapacityBytes;
    }

    public void setTotalCapacityBytes(long totalCapacityBytes) {
        this.totalCapacityBytes = totalCapacityBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public void setAvailableBytes(long availableBytes) {
        this.availableBytes = availableBytes;
    }

    public long getTrashBytes() {
        return trashBytes;
    }

    public void setTrashBytes(long trashBytes) {
        this.trashBytes = trashBytes;
    }
}

