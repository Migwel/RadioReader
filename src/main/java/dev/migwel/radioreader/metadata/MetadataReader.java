package dev.migwel.radioreader.metadata;

public interface MetadataReader {
    SongInfo retrieveMetadata(String streamUrl);
}
