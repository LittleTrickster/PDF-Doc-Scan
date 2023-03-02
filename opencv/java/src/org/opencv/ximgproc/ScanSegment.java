//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ximgproc;

import org.opencv.core.Algorithm;
import org.opencv.core.Mat;

// C++: class ScanSegment
/**
 * Class implementing the F-DBSCAN (Accelerated superpixel image segmentation with a parallelized DBSCAN algorithm) superpixels
 * algorithm by Loke SC, et al. CITE: loke2021accelerated for original paper.
 *
 * The algorithm uses a parallelised DBSCAN cluster search that is resistant to noise, competitive in segmentation quality, and faster than
 * existing superpixel segmentation methods. When tested on the Berkeley Segmentation Dataset, the average processing speed is 175 frames/s
 * with a Boundary Recall of 0.797 and an Achievable Segmentation Accuracy of 0.944. The computational complexity is quadratic O(n2) and
 * more suited to smaller images, but can still process a 2MP colour image faster than the SEEDS algorithm in OpenCV. The output is deterministic
 * when the number of processing threads is fixed, and requires the source image to be in Lab colour format.
 */
public class ScanSegment extends Algorithm {

    protected ScanSegment(long addr) { super(addr); }

    // internal usage only
    public static ScanSegment __fromPtr__(long addr) { return new ScanSegment(addr); }

    //
    // C++:  int cv::ximgproc::ScanSegment::getNumberOfSuperpixels()
    //

    /**
     * Returns the actual superpixel segmentation from the last image processed using iterate.
     *
     *     Returns zero if no image has been processed.
     * @return automatically generated
     */
    public int getNumberOfSuperpixels() {
        return getNumberOfSuperpixels_0(nativeObj);
    }


    //
    // C++:  void cv::ximgproc::ScanSegment::iterate(Mat img)
    //

    /**
     * Calculates the superpixel segmentation on a given image with the initialized
     *     parameters in the ScanSegment object.
     *
     *     This function can be called again for other images without the need of initializing the algorithm with createScanSegment().
     *     This save the computational cost of allocating memory for all the structures of the algorithm.
     *
     *     @param img Input image. Supported format: CV_8UC3. Image size must match with the initialized
     *     image size with the function createScanSegment(). It MUST be in Lab color space.
     */
    public void iterate(Mat img) {
        iterate_0(nativeObj, img.nativeObj);
    }


    //
    // C++:  void cv::ximgproc::ScanSegment::getLabels(Mat& labels_out)
    //

    /**
     * Returns the segmentation labeling of the image.
     *
     *     Each label represents a superpixel, and each pixel is assigned to one superpixel label.
     *
     *     @param labels_out Return: A CV_32UC1 integer array containing the labels of the superpixel
     *     segmentation. The labels are in the range [0, getNumberOfSuperpixels()].
     */
    public void getLabels(Mat labels_out) {
        getLabels_0(nativeObj, labels_out.nativeObj);
    }


    //
    // C++:  void cv::ximgproc::ScanSegment::getLabelContourMask(Mat& image, bool thick_line = false)
    //

    /**
     * Returns the mask of the superpixel segmentation stored in the ScanSegment object.
     *
     *     The function return the boundaries of the superpixel segmentation.
     *
     *     @param image Return: CV_8UC1 image mask where -1 indicates that the pixel is a superpixel border, and 0 otherwise.
     *     @param thick_line If false, the border is only one pixel wide, otherwise all pixels at the border are masked.
     */
    public void getLabelContourMask(Mat image, boolean thick_line) {
        getLabelContourMask_0(nativeObj, image.nativeObj, thick_line);
    }

    /**
     * Returns the mask of the superpixel segmentation stored in the ScanSegment object.
     *
     *     The function return the boundaries of the superpixel segmentation.
     *
     *     @param image Return: CV_8UC1 image mask where -1 indicates that the pixel is a superpixel border, and 0 otherwise.
     */
    public void getLabelContourMask(Mat image) {
        getLabelContourMask_1(nativeObj, image.nativeObj);
    }


    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }



    // C++:  int cv::ximgproc::ScanSegment::getNumberOfSuperpixels()
    private static native int getNumberOfSuperpixels_0(long nativeObj);

    // C++:  void cv::ximgproc::ScanSegment::iterate(Mat img)
    private static native void iterate_0(long nativeObj, long img_nativeObj);

    // C++:  void cv::ximgproc::ScanSegment::getLabels(Mat& labels_out)
    private static native void getLabels_0(long nativeObj, long labels_out_nativeObj);

    // C++:  void cv::ximgproc::ScanSegment::getLabelContourMask(Mat& image, bool thick_line = false)
    private static native void getLabelContourMask_0(long nativeObj, long image_nativeObj, boolean thick_line);
    private static native void getLabelContourMask_1(long nativeObj, long image_nativeObj);

    // native support for java finalize()
    private static native void delete(long nativeObj);

}
