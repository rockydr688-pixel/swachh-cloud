import cv2
import numpy as np
import pytest
from app.blur_detector import BlurDetector
from app.image_analyzer import ImageAnalyzer

@pytest.fixture
def detectors():
    bd = BlurDetector(heavy_threshold=100.0, moderate_threshold=250.0)
    ia = ImageAnalyzer(blur_detector=bd)
    return bd, ia

def create_synthetic_sharp_image(width=800, height=600):
    """Creates an image with high-contrast sharp checkerboard edges."""
    img = np.zeros((height, width, 3), dtype=np.uint8)
    # Draw high contrast sharp boxes and lines
    for x in range(0, width, 40):
        cv2.line(img, (x, 0), (x, height), (255, 255, 255), 2)
    for y in range(0, height, 40):
        cv2.line(img, (0, y), (width, y), (255, 255, 255), 2)
    cv2.rectangle(img, (100, 100), (300, 300), (0, 255, 0), -1)
    cv2.circle(img, (500, 300), 80, (0, 0, 255), -1)
    return img

def create_synthetic_blurry_image(sharp_img, ksize=51):
    """Applies strong Gaussian blur to a sharp image."""
    return cv2.GaussianBlur(sharp_img, (ksize, ksize), 0)

def create_synthetic_dark_image(width=800, height=600):
    """Creates a very dark image with average intensity < 20."""
    return np.random.randint(0, 15, size=(height, width, 3), dtype=np.uint8)

def create_synthetic_overexposed_image(width=800, height=600):
    """Creates an overexposed saturated image with intensity > 240."""
    return np.random.randint(240, 256, size=(height, width, 3), dtype=np.uint8)

def test_sharp_image_classification(detectors):
    bd, ia = detectors
    sharp_img = create_synthetic_sharp_image()
    _, buffer = cv2.imencode('.png', sharp_img)

    result = bd.analyze_image_bytes(buffer.tobytes())
    assert result["classification"] == "clear"
    assert not result["is_blurry"]
    assert result["laplacian_variance"] > 250.0
    assert result["blur_score"] >= 0.75

def test_blurry_image_classification(detectors):
    bd, ia = detectors
    sharp_img = create_synthetic_sharp_image()
    blurry_img = create_synthetic_blurry_image(sharp_img, ksize=61)
    _, buffer = cv2.imencode('.png', blurry_img)

    result = bd.analyze_image_bytes(buffer.tobytes())
    assert result["is_blurry"]
    assert result["classification"] in ["moderately_blurry", "heavily_blurry"]
    assert result["laplacian_variance"] < 250.0

def test_dark_image_detection(detectors):
    _, ia = detectors
    dark_img = create_synthetic_dark_image()
    _, buffer = cv2.imencode('.png', dark_img)

    analysis = ia.full_analysis(buffer.tobytes())
    assert analysis["exposure"]["is_very_dark"]
    assert analysis["exposure"]["exposure_status"] == "very_dark"
    assert analysis["is_flagged"]
    assert any("underexposure" in d for d in analysis["defects"])

def test_overexposed_image_detection(detectors):
    _, ia = detectors
    bright_img = create_synthetic_overexposed_image()
    _, buffer = cv2.imencode('.png', bright_img)

    analysis = ia.full_analysis(buffer.tobytes())
    assert analysis["exposure"]["is_overexposed"]
    assert analysis["exposure"]["exposure_status"] == "overexposed"
    assert analysis["is_flagged"]
    assert any("overexposure" in d for d in analysis["defects"])

def test_low_resolution_detection(detectors):
    _, ia = detectors
    tiny_img = np.zeros((200, 200, 3), dtype=np.uint8)
    _, buffer = cv2.imencode('.png', tiny_img)

    analysis = ia.full_analysis(buffer.tobytes())
    assert analysis["resolution"]["is_low_resolution"]
    assert analysis["dimensions"]["width"] == 200
    assert analysis["dimensions"]["height"] == 200
    assert analysis["dimensions"]["megapixels"] < 0.3

def test_perceptual_hashes(detectors):
    _, ia = detectors
    img1 = create_synthetic_sharp_image()
    # Identical image
    img2 = img1.copy()
    # Slightly resized copy (visually very similar)
    img3 = cv2.resize(img1, (780, 580))
    img3 = cv2.resize(img3, (800, 600))

    _, b1 = cv2.imencode('.png', img1)
    _, b2 = cv2.imencode('.png', img2)
    _, b3 = cv2.imencode('.png', img3)

    res1 = ia.full_analysis(b1.tobytes())
    res2 = ia.full_analysis(b2.tobytes())
    res3 = ia.full_analysis(b3.tobytes())

    # Exact copy must produce identical perceptual hashes
    assert res1["perceptual_hashes"]["dhash"] == res2["perceptual_hashes"]["dhash"]
    assert res1["perceptual_hashes"]["ahash"] == res2["perceptual_hashes"]["ahash"]

    # Visually similar resized copy must have very low Hamming distance
    h1 = int(res1["perceptual_hashes"]["dhash"], 16)
    h3 = int(res3["perceptual_hashes"]["dhash"], 16)
    hamming_dist = bin(h1 ^ h3).count('1')
    assert hamming_dist <= 5, f"Hamming distance {hamming_dist} should be <= 5 for similar images"
