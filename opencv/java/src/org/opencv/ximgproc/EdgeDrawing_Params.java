//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.ximgproc;



// C++: class Params

public class EdgeDrawing_Params {

    protected final long nativeObj;
    protected EdgeDrawing_Params(long addr) { nativeObj = addr; }

    public long getNativeObjAddr() { return nativeObj; }

    // internal usage only
    public static EdgeDrawing_Params __fromPtr__(long addr) { return new EdgeDrawing_Params(addr); }

    //
    // C++:   cv::ximgproc::EdgeDrawing::Params::Params()
    //

    public EdgeDrawing_Params() {
        nativeObj = EdgeDrawing_Params_0();
    }


    //
    // C++: bool EdgeDrawing_Params::PFmode
    //

    public boolean get_PFmode() {
        return get_PFmode_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::PFmode
    //

    public void set_PFmode(boolean PFmode) {
        set_PFmode_0(nativeObj, PFmode);
    }


    //
    // C++: int EdgeDrawing_Params::EdgeDetectionOperator
    //

    public int get_EdgeDetectionOperator() {
        return get_EdgeDetectionOperator_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::EdgeDetectionOperator
    //

    public void set_EdgeDetectionOperator(int EdgeDetectionOperator) {
        set_EdgeDetectionOperator_0(nativeObj, EdgeDetectionOperator);
    }


    //
    // C++: int EdgeDrawing_Params::GradientThresholdValue
    //

    public int get_GradientThresholdValue() {
        return get_GradientThresholdValue_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::GradientThresholdValue
    //

    public void set_GradientThresholdValue(int GradientThresholdValue) {
        set_GradientThresholdValue_0(nativeObj, GradientThresholdValue);
    }


    //
    // C++: int EdgeDrawing_Params::AnchorThresholdValue
    //

    public int get_AnchorThresholdValue() {
        return get_AnchorThresholdValue_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::AnchorThresholdValue
    //

    public void set_AnchorThresholdValue(int AnchorThresholdValue) {
        set_AnchorThresholdValue_0(nativeObj, AnchorThresholdValue);
    }


    //
    // C++: int EdgeDrawing_Params::ScanInterval
    //

    public int get_ScanInterval() {
        return get_ScanInterval_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::ScanInterval
    //

    public void set_ScanInterval(int ScanInterval) {
        set_ScanInterval_0(nativeObj, ScanInterval);
    }


    //
    // C++: int EdgeDrawing_Params::MinPathLength
    //

    public int get_MinPathLength() {
        return get_MinPathLength_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::MinPathLength
    //

    public void set_MinPathLength(int MinPathLength) {
        set_MinPathLength_0(nativeObj, MinPathLength);
    }


    //
    // C++: float EdgeDrawing_Params::Sigma
    //

    public float get_Sigma() {
        return get_Sigma_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::Sigma
    //

    public void set_Sigma(float Sigma) {
        set_Sigma_0(nativeObj, Sigma);
    }


    //
    // C++: bool EdgeDrawing_Params::SumFlag
    //

    public boolean get_SumFlag() {
        return get_SumFlag_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::SumFlag
    //

    public void set_SumFlag(boolean SumFlag) {
        set_SumFlag_0(nativeObj, SumFlag);
    }


    //
    // C++: bool EdgeDrawing_Params::NFAValidation
    //

    public boolean get_NFAValidation() {
        return get_NFAValidation_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::NFAValidation
    //

    public void set_NFAValidation(boolean NFAValidation) {
        set_NFAValidation_0(nativeObj, NFAValidation);
    }


    //
    // C++: int EdgeDrawing_Params::MinLineLength
    //

    public int get_MinLineLength() {
        return get_MinLineLength_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::MinLineLength
    //

    public void set_MinLineLength(int MinLineLength) {
        set_MinLineLength_0(nativeObj, MinLineLength);
    }


    //
    // C++: double EdgeDrawing_Params::MaxDistanceBetweenTwoLines
    //

    public double get_MaxDistanceBetweenTwoLines() {
        return get_MaxDistanceBetweenTwoLines_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::MaxDistanceBetweenTwoLines
    //

    public void set_MaxDistanceBetweenTwoLines(double MaxDistanceBetweenTwoLines) {
        set_MaxDistanceBetweenTwoLines_0(nativeObj, MaxDistanceBetweenTwoLines);
    }


    //
    // C++: double EdgeDrawing_Params::LineFitErrorThreshold
    //

    public double get_LineFitErrorThreshold() {
        return get_LineFitErrorThreshold_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::LineFitErrorThreshold
    //

    public void set_LineFitErrorThreshold(double LineFitErrorThreshold) {
        set_LineFitErrorThreshold_0(nativeObj, LineFitErrorThreshold);
    }


    //
    // C++: double EdgeDrawing_Params::MaxErrorThreshold
    //

    public double get_MaxErrorThreshold() {
        return get_MaxErrorThreshold_0(nativeObj);
    }


    //
    // C++: void EdgeDrawing_Params::MaxErrorThreshold
    //

    public void set_MaxErrorThreshold(double MaxErrorThreshold) {
        set_MaxErrorThreshold_0(nativeObj, MaxErrorThreshold);
    }


    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }



    // C++:   cv::ximgproc::EdgeDrawing::Params::Params()
    private static native long EdgeDrawing_Params_0();

    // C++: bool EdgeDrawing_Params::PFmode
    private static native boolean get_PFmode_0(long nativeObj);

    // C++: void EdgeDrawing_Params::PFmode
    private static native void set_PFmode_0(long nativeObj, boolean PFmode);

    // C++: int EdgeDrawing_Params::EdgeDetectionOperator
    private static native int get_EdgeDetectionOperator_0(long nativeObj);

    // C++: void EdgeDrawing_Params::EdgeDetectionOperator
    private static native void set_EdgeDetectionOperator_0(long nativeObj, int EdgeDetectionOperator);

    // C++: int EdgeDrawing_Params::GradientThresholdValue
    private static native int get_GradientThresholdValue_0(long nativeObj);

    // C++: void EdgeDrawing_Params::GradientThresholdValue
    private static native void set_GradientThresholdValue_0(long nativeObj, int GradientThresholdValue);

    // C++: int EdgeDrawing_Params::AnchorThresholdValue
    private static native int get_AnchorThresholdValue_0(long nativeObj);

    // C++: void EdgeDrawing_Params::AnchorThresholdValue
    private static native void set_AnchorThresholdValue_0(long nativeObj, int AnchorThresholdValue);

    // C++: int EdgeDrawing_Params::ScanInterval
    private static native int get_ScanInterval_0(long nativeObj);

    // C++: void EdgeDrawing_Params::ScanInterval
    private static native void set_ScanInterval_0(long nativeObj, int ScanInterval);

    // C++: int EdgeDrawing_Params::MinPathLength
    private static native int get_MinPathLength_0(long nativeObj);

    // C++: void EdgeDrawing_Params::MinPathLength
    private static native void set_MinPathLength_0(long nativeObj, int MinPathLength);

    // C++: float EdgeDrawing_Params::Sigma
    private static native float get_Sigma_0(long nativeObj);

    // C++: void EdgeDrawing_Params::Sigma
    private static native void set_Sigma_0(long nativeObj, float Sigma);

    // C++: bool EdgeDrawing_Params::SumFlag
    private static native boolean get_SumFlag_0(long nativeObj);

    // C++: void EdgeDrawing_Params::SumFlag
    private static native void set_SumFlag_0(long nativeObj, boolean SumFlag);

    // C++: bool EdgeDrawing_Params::NFAValidation
    private static native boolean get_NFAValidation_0(long nativeObj);

    // C++: void EdgeDrawing_Params::NFAValidation
    private static native void set_NFAValidation_0(long nativeObj, boolean NFAValidation);

    // C++: int EdgeDrawing_Params::MinLineLength
    private static native int get_MinLineLength_0(long nativeObj);

    // C++: void EdgeDrawing_Params::MinLineLength
    private static native void set_MinLineLength_0(long nativeObj, int MinLineLength);

    // C++: double EdgeDrawing_Params::MaxDistanceBetweenTwoLines
    private static native double get_MaxDistanceBetweenTwoLines_0(long nativeObj);

    // C++: void EdgeDrawing_Params::MaxDistanceBetweenTwoLines
    private static native void set_MaxDistanceBetweenTwoLines_0(long nativeObj, double MaxDistanceBetweenTwoLines);

    // C++: double EdgeDrawing_Params::LineFitErrorThreshold
    private static native double get_LineFitErrorThreshold_0(long nativeObj);

    // C++: void EdgeDrawing_Params::LineFitErrorThreshold
    private static native void set_LineFitErrorThreshold_0(long nativeObj, double LineFitErrorThreshold);

    // C++: double EdgeDrawing_Params::MaxErrorThreshold
    private static native double get_MaxErrorThreshold_0(long nativeObj);

    // C++: void EdgeDrawing_Params::MaxErrorThreshold
    private static native void set_MaxErrorThreshold_0(long nativeObj, double MaxErrorThreshold);

    // native support for java finalize()
    private static native void delete(long nativeObj);

}
