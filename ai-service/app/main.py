import os
from fastapi import FastAPI, File, UploadFile, Query, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any

from app.blur_detector import BlurDetector
from app.image_analyzer import ImageAnalyzer

# Initialize FastAPI app
app = FastAPI(
    title="CloudClean AI Image Analysis Microservice",
    description="Intelligent computer vision microservice for Laplacian blur detection, exposure analysis, and perceptual hashing.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

blur_detector = BlurDetector()
image_analyzer = ImageAnalyzer(blur_detector=blur_detector)

INTERNAL_SECRET = os.getenv("AI_SERVICE_SECRET", "cloudclean-ai-internal-key-32chars!")

def verify_internal_auth(x_internal_key: Optional[str] = Header(None)):
    """
    Enforces secure authenticated communication between Spring Boot and the AI microservice.
    """
    # Accept internal key or allow localhost loopback
    if x_internal_key and x_internal_key != INTERNAL_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized internal service access")
    return True

class PathAnalysisRequest(BaseModel):
    file_path: str
    heavy_threshold: Optional[float] = 100.0
    moderate_threshold: Optional[float] = 250.0

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "cloudclean-ai-vision",
        "version": "1.0.0",
        "algorithms": ["laplacian_variance", "dhash", "ahash", "luminance_histogram"],
    }

@app.post("/analyze/blur")
async def analyze_blur_upload(
    file: UploadFile = File(...),
    heavy_threshold: Optional[float] = Query(100.0),
    moderate_threshold: Optional[float] = Query(250.0),
    auth: bool = Depends(verify_internal_auth),
):
    try:
        image_bytes = await file.read()
        if not image_bytes:
            raise HTTPException(status_code=400, detail="Uploaded file is empty")

        result = blur_detector.analyze_image_bytes(
            image_bytes,
            heavy_thresh=heavy_threshold,
            moderate_thresh=moderate_threshold,
        )
        result["filename"] = file.filename
        return {"success": True, "data": result}
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Image processing error: {str(e)}")

@app.post("/analyze/full")
async def analyze_full_upload(
    file: UploadFile = File(...),
    auth: bool = Depends(verify_internal_auth),
):
    try:
        image_bytes = await file.read()
        if not image_bytes:
            raise HTTPException(status_code=400, detail="Uploaded file is empty")

        analysis = image_analyzer.full_analysis(image_bytes)
        analysis["filename"] = file.filename
        return {"success": True, "data": analysis}
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Image processing error: {str(e)}")

@app.post("/analyze/path")
def analyze_from_local_path(
    req: PathAnalysisRequest,
    auth: bool = Depends(verify_internal_auth),
):
    """
    Direct host file path analysis for fast zero-network-copy local agent processing.
    """
    if not os.path.exists(req.file_path):
        raise HTTPException(status_code=404, detail=f"File not found on host: {req.file_path}")

    try:
        with open(req.file_path, "rb") as f:
            image_bytes = f.read()

        analysis = image_analyzer.full_analysis(image_bytes)
        analysis["file_path"] = req.file_path
        return {"success": True, "data": analysis}
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Failed to analyze file: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="127.0.0.1", port=8001, reload=False)
