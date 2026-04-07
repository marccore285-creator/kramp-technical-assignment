variable "project_id" {
  description = "GCP project ID where resources will be created"
  type        = string
  # Set via: terraform apply -var="project_id=my-gcp-project"
}

variable "region" {
  description = "GCP region for Cloud Run and Artifact Registry"
  type        = string
  default     = "europe-west1"  # Netherlands — close to Kramp's customer base
}

variable "service_name" {
  description = "Cloud Run service name (also used as Artifact Registry repository name)"
  type        = string
  default     = "product-info-aggregator"
}
