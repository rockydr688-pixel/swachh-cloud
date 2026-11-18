import cv2
import numpy as np
from typing import Tuple, Dict, Any, Optional

class BlurDetector:
    """
    Intelligent image blur detector using Variance of Laplacian algorithm.
    Calibrated for photograph inspection and SaaS storage cleanup.
    """

    DEFAULT_HEAVY_THRESHOLD = 100.0
    DEFAULT_MODERATE_THRESHOLD = 250.0

    def __init__(
        self,
        heavy_threshold: float = DEFAULT_HEAVY_THRESHOLD,
        moderate_threshold: float = DEFAULT_MODERATE_THRESHOLD,
    ):
        self.heavy_threshold = heavy_threshold
        self.moderate_threshold = moderate_threshold

    def calculate_laplacian_variance(self, image_np: np.ndarray) -> float:
        """
        Computes the variance of the Laplacian of a grayscale image.
        Higher values indicate high frequency edges (sharp image);
        Low values indicate lack of edges (blurred image).
        """
        if len(image_np.shape) == 3:
            gray = cv2.cvtColor(image_np, cv2.COLOR_BGR2GRAY)
        else:
            gray = image_np

        # Apply 64-bit float Laplacian operator
        laplacian = cv2.Laplacian(gray, cv2.CV_64F)
        variance = float(laplacian.var())
        return round(variance, 2)

    def classify_blur(
        self,
        variance: float,
        heavy_thresh: Optional[float] = None,
        moderate_thresh: Optional[float] = None,
    ) -> Tuple[str, bool, float]:
        """
        Classifies the image blur based on calibrated thresholds.
        Returns:
            classification: 'clear' | 'moderately_blurry' | 'heavily_blurry'
            is_blurry: True if moderately or heavily blurry
            normalized_score: 0.0 (completely blurry) to 1.0 (crisp/clear)
        """
        h_thresh = heavy_thresh if heavy_thresh is not None else self.heavy_threshold
        m_thresh = moderate_thresh if moderate_thresh is not None else self.moderate_threshold

        if variance < h_thresh:
            classification = "heavily_blurry"
            is_blurry = True
            # Normalized score mapped to 0.0 - 0.4
            norm_score = max(0.0, min(0.4, (variance / max(h_thresh, 1.0)) * 0.4))
        elif variance < m_thresh:
            classification = "moderately_blurry"
            is_blurry = True
            # Normalized score mapped to 0.4 - 0.75
            norm_score = 0.4 + ((variance - h_thresh) / max(m_thresh - h_thresh, 1.0)) * 0.35
        else:
            classification = "clear"
            is_blurry = False
            # Normalized score mapped to 0.75 - 1.0
            norm_score = min(1.0, 0.75 + min(0.25, (variance - m_thresh) / 1000.0 * 0.25))

        return classification, is_blurry, round(norm_score, 3)

    def analyze_image_bytes(
        self,
        image_bytes: bytes,
        heavy_thresh: Optional[float] = None,
        moderate_thresh: Optional[float] = None,
    ) -> Dict[str, Any]:
        """
        Decodes raw bytes and returns comprehensive blur analysis.
        """
        if not image_bytes or len(image_bytes) == 0:
            raise ValueError("Could not decode image bytes into valid picture array: empty buffer")

        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Could not decode image bytes into valid picture array")

        variance = self.calculate_laplacian_variance(img)
        classification, is_blurry, norm_score = self.classify_blur(variance, heavy_thresh, moderate_thresh)

        return {
            "laplacian_variance": variance,
            "blur_score": norm_score,
            "classification": classification,
            "is_blurry": is_blurry,
            "thresholds": {
                "heavy": heavy_thresh or self.heavy_threshold,
                "moderate": moderate_thresh or self.moderate_threshold,
            },
        }
