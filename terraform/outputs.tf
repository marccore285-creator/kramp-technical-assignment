output "service_url" {
  description = "HTTPS URL of the deployed Cloud Run service (REST endpoint base)"
  value       = google_cloud_run_v2_service.aggregator.uri
}

output "service_name" {
  description = "Cloud Run service name"
  value       = google_cloud_run_v2_service.aggregator.name
}

output "image_repository" {
  description = "Artifact Registry repository URL"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${var.service_name}"
}
