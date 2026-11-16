# CloudClean AI - Vision Microservice

FastAPI microservice providing computer vision intelligence for CloudClean AI:
- **Laplacian Variance Blur Detection**: Categorizes photographs into `clear`, `moderately_blurry`, or `heavily_blurry`.
- **Exposure Diagnostics**: Flags severely under-exposed (very dark) or over-exposed (blown out) photographs.
- **Resolution Verification**: Identifies low-resolution thumbnails or corrupted dimension headers.
- **Perceptual Hashing**: Computes 64-bit difference hash (dHash) and average hash (aHash) for visual similarity matching.
- **Composite Quality Scoring**: Weighted quality score (0 - 100).

## Requirements
- Python 3.10+
- FastAPI, OpenCV (`opencv-python-headless`), NumPy, Pillow

## Running Locally
```bash
pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

## Running Tests
```bash
pytest test_ai_service.py -v
```
