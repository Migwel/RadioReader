package dev.migwel.radioreader.icy;

import dev.migwel.radioreader.metadata.Metadata;
import dev.migwel.radioreader.metadata.SongInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IcyMetadataReader {

    private static final Logger log = LoggerFactory.getLogger(IcyMetadataReader.class);

    public static void main(String[] args) {
        System.out.println(new IcyMetadataReader().retrieveMetadata("http://maximum.belstream.net/maximum.mp3"));
        System.out.println(new IcyMetadataReader().retrieveMetadata("https://icecast.omroep.nl/radio2-bb-mp3"));
        System.out.println(new IcyMetadataReader().retrieveMetadata("https://www.mp3streams.nl/zender/qmusic/stream/20-aac-64"));
    }

    public SongInfo retrieveMetadata(String streamUrl) {
        HttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(streamUrl);
        httpGet.addHeader("Icy-MetaData", "1");
        httpGet.addHeader("Connection", "close");
        httpGet.addHeader("Accept", "");
        HttpResponse response;
        try {
            response = client.execute(httpGet);
        } catch (IOException e) {
            log.warn("An exception occurred while fetching the stream", e);
            return null;
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            log.warn("Could not fetch stream. Status line: "+ response.getStatusLine());
            return null;
        }

        int metadataOffset = retrieveMetadataOffset(response);

        if (metadataOffset == 0) {
            log.info("Could not find metadata for url:"+ streamUrl);
            return null;
        }

        List<Metadata> metadata = extractMetadata(response, metadataOffset);
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return extractSongInfo(metadata);
    }

    private List<Metadata> extractMetadata(HttpResponse response, int metadataOffset) {
        String metadataStr;
        try(InputStream stream = response.getEntity().getContent()) {
            if (stream.skip(metadataOffset) != metadataOffset) {
                return null;
            }
            int metaDataLength = stream.read() * 16;
            metadataStr = getMetadataStr(stream, metaDataLength);
            if (metadataStr == null) {
                return null;
            }
        } catch (IOException e) {
            log.warn("Something went wrong while reading the stream", e);
            return null;
        }
        return Arrays.stream(metadataStr.split(";"))
                     .map(e -> e.split("=", 2))
                     .filter(e -> e.length == 2)
                     .map(e -> new Metadata(e[0], e[1].substring(1, e[1].length()-1)))
                     .collect(Collectors.toList());
    }

    private String getMetadataStr(InputStream stream, int metaDataLength) throws IOException {
        byte[] b = new byte[metaDataLength];
        if (stream.read(b, 0, metaDataLength) != metaDataLength) {
            return null;
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    private int retrieveMetadataOffset(HttpResponse response) {
        if (!"".equals(response.getFirstHeader("icy-metaint").getValue())) {
            String icyMetaInt = response.getFirstHeader("icy-metaint").getValue();
            icyMetaInt = icyMetaInt.replace("[", "");
            icyMetaInt = icyMetaInt.replace("]", "");

            return Integer.parseInt(icyMetaInt);
        }
        return 0;
    }

    private SongInfo extractSongInfo(List<Metadata> metadataList) {
        for (Metadata metadata : metadataList) {
            if ("StreamTitle".equals(metadata.getKey())) {
                String[] songInfoStr = metadata.getValue().split(" - ", 2);
                return new SongInfo(songInfoStr[0], songInfoStr[1]);
            }
        }
        return null;
    }
}
