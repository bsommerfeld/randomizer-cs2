package de.bsommerfeld.randomizer.ui.util;

import lombok.Getter;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** A class to decode GIF files and retrieve information such as frames, duration, etc. */
public class GifDecoder {

    private final List<BufferedImage> frames = new ArrayList<>();
    private final List<Integer> frameDurations = new ArrayList<>();

    @Getter
    private int totalDuration;

    public GifDecoder(String gifResourcePath) throws IOException {
        decodeGifFile(gifResourcePath);
        calculateTotalDuration();
    }

    /**
     * Decodes the provided GIF file into individual frames and their respective durations.
     *
     * @param gifFile the GIF file to be decoded
     *
     * @throws IOException if an error occurs during reading the GIF file
     */
    private void decodeGifFile(String gifResourcePath) throws IOException {
        try (InputStream inputStream =
                     getClass().getClassLoader().getResourceAsStream(gifResourcePath)) {
            if (inputStream == null) {
                throw new IOException("GIF resource not found: " + gifResourcePath);
            }
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(ImageIO.createImageInputStream(inputStream), false);

            int numFrames = reader.getNumImages(true);
            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                frames.add(frame);

                IIOMetadata metadata = reader.getImageMetadata(i);
                int frameDuration = extractFrameDuration(metadata);
                frameDurations.add(frameDuration);
            }
        }
    }

    /**
     * Extracts the duration of a frame from its metadata.
     *
     * @param metadata the metadata of the frame from which to extract the duration
     *
     * @return the duration of the frame in milliseconds
     *
     * @throws IllegalArgumentException if the metadata format is not recognized
     */
    private int extractFrameDuration(IIOMetadata metadata) {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        if (!"javax_imageio_gif_image_1.0".equals(metaFormatName)) {
            throw new IllegalArgumentException("Unrecognized GIF metadata format: " + metaFormatName);
        }

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
        NodeList graphicControlExtensions = root.getElementsByTagName("GraphicControlExtension");

        if (graphicControlExtensions.getLength() > 0) {
            IIOMetadataNode node = (IIOMetadataNode) graphicControlExtensions.item(0);
            String delayTime = node.getAttribute("delayTime");
            return Integer.parseInt(delayTime) * 10;
        }
        return 100;
    }

    private void calculateTotalDuration() {
        totalDuration = frameDurations.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Retrieves a specific frame from the GIF based on the provided frame index.
     *
     * @param frameIndex the index of the frame to retrieve.
     *
     * @return the BufferedImage representing the specified frame.
     *
     * @throws IndexOutOfBoundsException if the frameIndex is out of the valid range.
     */
    public BufferedImage getFrame(int frameIndex) {
        validateFrameIndex(frameIndex, frames.size());
        return frames.get(frameIndex);
    }

    /**
     * Returns the total number of frames in the decoded GIF.
     *
     * @return the number of frames in the GIF
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Returns the duration of the specified frame in milliseconds.
     *
     * @param frameIndex the index of the frame for which the duration is to be retrieved
     *
     * @return the duration of the specified frame in milliseconds
     *
     * @throws IndexOutOfBoundsException if the frame index is out of bounds
     */
    public int getFrameDuration(int frameIndex) {
        validateFrameIndex(frameIndex, frameDurations.size());
        return frameDurations.get(frameIndex);
    }

    /**
     * Retrieves all frames from the decoded GIF file.
     *
     * @return a list of BufferedImage objects representing the frames of the GIF.
     */
    public List<BufferedImage> getAllFrames() {
        return frames;
    }

    private void validateFrameIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Frame index out of bounds: " + index);
        }
    }
}
