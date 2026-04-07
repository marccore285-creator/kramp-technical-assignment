terraform {
  required_version = ">= 1.6"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# ── Artifact Registry ─────────────────────────────────────────────────────────
resource "google_artifact_registry_repository" "aggregator" {
  location      = var.region
  repository_id = var.service_name
  format        = "DOCKER"
  description   = "Docker images for Product Info Aggregator"
}

# ── Cloud Run Service ─────────────────────────────────────────────────────────
resource "google_cloud_run_v2_service" "aggregator" {
  name     = var.service_name
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      # Image pushed by CI/CD pipeline; override with -var="image=..." on first deploy
      image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.service_name}/${var.service_name}:latest"

      ports {
        container_port = 8080
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }

      resources {
        limits = {
          cpu    = "1000m"
          memory = "512Mi"
        }
        # Allocates full CPU during startup to reduce cold-start latency
        startup_cpu_boost = true
      }

      startup_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 3
      }

      liveness_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        period_seconds    = 15
        failure_threshold = 3
      }
    }

    scaling {
      # Scale to zero when idle (cost saving for a demo/dev environment)
      min_instance_count = 0
      max_instance_count = 10
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_artifact_registry_repository.aggregator]
}

# ── IAM: public access ────────────────────────────────────────────────────────
# Remove this block and add proper authentication for a production deployment
resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.aggregator.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
