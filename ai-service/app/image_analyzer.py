import cv2
import numpy as np
from typing import Dict, Any, Tuple
from app.blur_detector import BlurDetector

class ImageAnalyzer:
    """
    Comprehensive computer vision image analyzer.
    Performs dimension extraction, resolution checks, darkness and overexposure detection,
    perceptual hashing (dHash/aHash), and overall quality scoring.
    """

    def __init__(self, blur_detector: BlurDetector = None):
        self.blur_detector = blur_detector or BlurDetector()

    def extract_dimensions(self, img: np.ndarray) -> Dict[str, Any]:
        height, width = img.shape[:2]
        channels = img.shape[2] if len(img.shape) > 2 else 1
        megapixels = round((width * height) / 1_000_000.0, 3)
        aspect_ratio = round(width / max(height, 1), 3)

        return {
            "width": width,
            "height": height,
            "channels": channels,
            "megapixels": megapixels,
            "aspect_ratio": aspect_ratio,
        }

    def check_resolution(self, width: int, height: int, megapixels: float) -> Tuple[bool, str]:
        """
        Flags images that are unusually small or thumbnail-sized.
        """
        if width < 640 or height < 480 or megapixels < 0.3:
            return True, f"Low resolution ({width}x{height}, {megapixels} MP < 0.3 MP baseline)"
        return False, "Standard resolution"

    def analyze_exposure(self, gray: np.ndarray) -> Dict[str, Any]:
        """
        Evaluates mean pixel brightness and histogram extremes to detect
        under-exposed (very dark) or over-exposed (blown out) photographs.
        """
        mean_intensity = float(np.mean(gray))
        std_dev = float(np.std(gray))

        total_pixels = gray.size
        dark_pixels = np.count_nonzero(gray < 25)
        bright_pixels = np.count_nonzero(gray > 240)

        dark_ratio = dark_pixels / total_pixels
        bright_ratio = bright_pixels / total_pixels

        is_very_dark = mean_intensity < 35.0 or dark_ratio > 0.70
        is_overexposed = mean_intensity > 220.0 or bright_ratio > 0.40

        exposure_status = "balanced"
        if is_very_dark:
            exposure_status = "very_dark"
        elif is_overexposed:
            exposure_status = "overexposed"

        return {
            "mean_intensity": round(mean_intensity, 2),
            "std_dev": round(std_dev, 2),
            "dark_pixel_ratio": round(dark_ratio, 3),
            "bright_pixel_ratio": round(bright_ratio, 3),
            "is_very_dark": is_very_dark,
            "is_overexposed": is_overexposed,
            "exposure_status": exposure_status,
        }

    def compute_perceptual_hashes(self, gray: np.ndarray) -> Dict[str, str]:
        """
        Computes 64-bit difference hash (dHash) and average hash (aHash)
        for perceptual visual similarity matching.
        """
        # 1. dHash (Difference Hash, 9x8)
        resized_d = cv2.resize(gray, (9, 8), interpolation=cv2.INTER_AREA)
        diff = resized_d[:, 1:] > resized_d[:, :-1]
        dhash_val = 0
        for bit in diff.flatten():
            dhash_val = (dhash_val << 1) | int(bit)
        dhash_hex = f"{dhash_val:016x}"

        # 2. aHash (Average Hash, 8x8)
        resized_a = cv2.resize(gray, (8, 8), interpolation=cv2.INTER_AREA)
        avg = resized_a.mean()
        ahash_val = 0
        for pixel in resized_a.flatten():
            ahash_val = (ahash_val << 1) | int(pixel > avg)
        ahash_hex = f"{ahash_val:016x}"

        return {
            "dhash": dhash_hex,
            "ahash": ahash_hex,
        }

    def compute_overall_quality_score(
        self,
        blur_score: float,
        is_blurry: bool,
        is_low_res: bool,
        is_dark: bool,
        is_overexposed: bool,
    ) -> float:
        """
        Computes an aggregate quality score on a 0 - 100 scale.
        """
        score = blur_score * 60.0  # Base clarity contribution up to 60 pts

        if not is_low_res:
            score += 20.0
        else:
            score += 5.0

        if not is_dark and not is_overexposed:
            score += 20.0
        elif is_dark or is_overexposed:
            score += 5.0

        return round(max(5.0, min(100.0, score)), 1)

    def full_analysis(self, image_bytes: bytes) -> Dict[str, Any]:
        """
        Executes end-to-end multi-dimensional image evaluation.
        """
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Unable to decode input image stream")

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        dims = self.extract_dimensions(img)
        is_low_res, res_reason = self.check_resolution(dims["width"], dims["height"], dims["megapixels"])
        exposure = self.analyze_exposure(gray)
        blur_info = self.blur_detector.analyze_image_bytes(image_bytes)
        phashes = self.compute_perceptual_hashes(gray)

        quality_score = self.compute_overall_quality_score(
            blur_info["blur_score"],
            blur_info["is_blurry"],
            is_low_res,
            exposure["is_very_dark"],
            exposure["is_overexposed"],
        )

        # Flagged if any defective trait is identified
        defects = []
        if blur_info["is_blurry"]:
            defects.append(f"Image is {blur_info['classification']} (variance: {blur_info['laplacian_variance']})")
        if is_low_res:
            defects.append(res_reason)
        if exposure["is_very_dark"]:
            defects.append(f"Severe underexposure (mean luminance: {exposure['mean_intensity']})")
        if exposure["is_overexposed"]:
            defects.append(f"Severe overexposure (mean luminance: {exposure['mean_intensity']})")

        return {
            "dimensions": dims,
            "resolution": {
                "is_low_resolution": is_low_res,
                "reason": res_reason,
            },
            "blur": blur_info,
            "exposure": exposure,
            "perceptual_hashes": phashes,
            "quality_score": quality_score,
            "is_flagged": len(defects) > 0,
            "defects": defects,
        }
