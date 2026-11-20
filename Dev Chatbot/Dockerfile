FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    HF_HOME=/opt/lia/.cache/huggingface

RUN useradd --create-home --uid 10001 --home-dir /opt/lia lia
WORKDIR /opt/lia

COPY requirements.txt ./
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch && \
    pip install --no-cache-dir -r requirements.txt

COPY --chown=lia:lia app.py banco_vetorial.py Talisma_Base_Conhecimento.pdf ./
USER lia
RUN python banco_vetorial.py
ENV HF_HUB_OFFLINE=1

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
