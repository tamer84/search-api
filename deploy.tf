# ========================================
# Variables
# ========================================
variable "application_version" {
  type = string
}

variable "application_name" {
  type    = string
  default = "search-api"
}

variable "aws_region" {
  type    = string
  default = "eu-central-1"
}

# ========================================
# Initialization
# ========================================
terraform {
  // Declares where terraform stores the application state
  backend "s3" {
    encrypt        = "true"
    bucket         = "tango-terraform"
    key            = "resources/search-api/tfstate.tf"
    region         = "eu-central-1"
    dynamodb_table = "terraform"
  }
}

provider "aws" {
  // Use the AWS provider from terraform https://www.terraform.io/docs/providers/aws/index.html
  region = "eu-central-1"
}

provider "github" {
  token        = data.terraform_remote_state.account_resources.outputs.github_access_token
}


# ========================================
# Remote States
# ========================================
data "terraform_remote_state" "account_resources" {
  // Imports the account resources to use the shared information
  backend = "s3"
  config = {
    encrypt = "true"
    bucket  = "tango-terraform"
    key     = "account_resources/tfstate.tf"
    region  = "eu-central-1"
  }
  workspace = "default"
}

data "terraform_remote_state" "terraform_build_image_resources" {
  backend = "s3"
  config = {
    encrypt = "true"
    bucket  = "tango-terraform"
    key     = "resources/terraform-build-image/tfstate.tf"
    region  = "eu-central-1"
  }
  workspace = terraform.workspace
}

data "terraform_remote_state" "environment_resources" {
  // Imports the environment specific resources to use the shared information
  backend = "s3"
  config = {
    encrypt = "true"
    bucket  = "tango-terraform"
    key     = "environment_resources/tfstate.tf"
    region  = "eu-central-1"
  }
  workspace = terraform.workspace
}

data "aws_caller_identity" "current" {}

# ========================================
# Locals
# ========================================
locals {
  code = "target/${var.application_name}-${var.application_version}.jar"
  apis = {
    graphql = {
      path1 = "graphql"
    }
  }
  default_responses = {
    "DEFAULT_4XX"   = { "type" = "DEFAULT_4XX", "status_code" = "" }
    "DEFAULT_5XX"   = { "type" = "DEFAULT_5XX", "status_code" = "" }
    "UNAUTHORIZED"  = { "type" = "UNAUTHORIZED", "status_code" = "401" }
    "ACCESS_DENIED" = { "type" = "ACCESS_DENIED", "status_code" = "403" }
  }
  cicd_branch = contains(["dev", "test", "int"], terraform.workspace) ? "develop" : "main"
}

# ========================================
# REST API General
# ========================================
resource "aws_api_gateway_rest_api" "search-api" {
  name = "${var.application_name}-${terraform.workspace}"

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = {
    Terraform   = "true"
    Environment = terraform.workspace
  }
}

resource "aws_api_gateway_gateway_response" "default_responses" {
  for_each = local.default_responses

  rest_api_id   = aws_api_gateway_rest_api.search-api.id
  response_type = each.value["type"]
  status_code   = each.value["status_code"]

  response_parameters = {
    "gatewayresponse.header.Access-Control-Allow-Headers" = "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,Timestamp,x-api-trace-id,x-api-request-id,x-api-response-time'",
    "gatewayresponse.header.Access-Control-Allow-Methods" = "'GET,OPTIONS,POST'",
    "gatewayresponse.header.Access-Control-Allow-Origin"  = "'*'"
  }

  response_templates = {
    "application/json" = "{\"message\":$context.error.messageString}"
  }
}

# ========================================
# Personal authenticated endpoint setup
# ========================================
resource "aws_api_gateway_resource" "graphql" {
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  path_part   = "graphql"
  // this resource is linked to api itself
  parent_id = aws_api_gateway_rest_api.search-api.root_resource_id

  depends_on = [
    aws_api_gateway_rest_api.search-api
  ]
}

// The method is the next part of the api definition, the methods can be declared independent,
resource "aws_api_gateway_method" "method" {
  // https://www.terraform.io/docs/providers/aws/r/api_gateway_method.html
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  resource_id = aws_api_gateway_resource.graphql.id
  http_method = "POST"
  authorization = "NONE"
  depends_on = [
    aws_api_gateway_resource.graphql
  ]
}

resource "aws_api_gateway_method" "options_method" {
  rest_api_id   = aws_api_gateway_rest_api.search-api.id
  resource_id   = aws_api_gateway_resource.graphql.id
  http_method   = "OPTIONS"
  authorization = "NONE"
  depends_on = [
    aws_api_gateway_resource.graphql
  ]
}

resource "aws_api_gateway_method_response" "options_200" {
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  resource_id = aws_api_gateway_resource.graphql.id
  http_method = aws_api_gateway_method.options_method.http_method
  status_code = "200"

  response_models = {
    "application/json" = "Empty"
  }

  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = true,
    "method.response.header.Access-Control-Allow-Methods" = false,
    "method.response.header.Access-Control-Allow-Origin"  = true
  }
}

resource "aws_api_gateway_integration" "cors" {
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  resource_id = aws_api_gateway_resource.graphql.id
  http_method = aws_api_gateway_method.options_method.http_method
  type        = "MOCK"

  request_templates = {
    "application/json" = jsonencode({ statusCode = 200 })
  }

  depends_on = [
    aws_api_gateway_method.options_method
  ]
}

resource "aws_api_gateway_integration_response" "cors" {
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  resource_id = aws_api_gateway_resource.graphql.id
  http_method = aws_api_gateway_method.options_method.http_method
  status_code = aws_api_gateway_method_response.options_200.status_code

  response_templates = {
    "application/json" = ""
  }

  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,Timestamp'",
    "method.response.header.Access-Control-Allow-Methods" = "'GET,OPTIONS,POST'",
    "method.response.header.Access-Control-Allow-Origin"  = "'*'"
  }

  depends_on = [
    aws_api_gateway_method_response.options_200
  ]
}


// This is what ties the API Gateway with the Lambda https://www.terraform.io/docs/providers/aws/r/api_gateway_integration.html
resource "aws_api_gateway_integration" "search_api_integration" {
  rest_api_id = aws_api_gateway_rest_api.search-api.id
  resource_id = aws_api_gateway_resource.graphql.id
  http_method = aws_api_gateway_method.method.http_method
  // Unless really necessary, for lambda integration, AWS_PROXY should be used
  type = "AWS_PROXY"
  // This is the lambda URL on AWS infra, using the invoke_arn we guarantee that we are invoking the latest version of it
  uri = aws_lambda_function.search-graphql.invoke_arn
  // For AWS_PROXY on lambda always use POST
  integration_http_method = "POST"
}

// This permission is necessary to allow the API gateway to invoke the lambda https://www.terraform.io/docs/providers/aws/r/lambda_permission.html
resource "aws_lambda_permission" "search_graphql_permission" {
  statement_id  = "AllowDealerApiToInvokeLambda${terraform.workspace}"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.search-graphql.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.search-api.execution_arn}/*/*/*"
}

// Until here the API itself was created, now we control the deployment of the API

# ========================================
# REST API - DEPLOYMENT
# ========================================

// This resources deploys the api when any of the variables change, in this case when the application version changes
// https://www.terraform.io/docs/providers/aws/r/api_gateway_deployment.html
resource "aws_api_gateway_deployment" "deployment" {
  description = "Deployed by Terraform"

  depends_on = [
    aws_api_gateway_integration.search_api_integration,
    aws_api_gateway_method.method,
  ]

  rest_api_id = aws_api_gateway_rest_api.search-api.id

  variables = {
    "app_version" = var.application_version
    "lambda_version" = aws_lambda_function.search-graphql.version
  }

  lifecycle {
    create_before_destroy = true
  }
}

// This log group is created to make it easier to log the API calls, all the calls to this API will be saved under /api/{api-name} log group
resource "aws_cloudwatch_log_group" "search-api-logs" {
  name = "/api/${aws_api_gateway_rest_api.search-api.name}"

  tags = {
    Terraform   = "true"
    Environment = terraform.workspace
  }
}

// The API stage allows you to have multiple deployments for the same API, like one for each environment for instance
// On Tango instead of having one stage per environment we have one API per environment, therefore by default the API has only the "live" stage
// During test one can create another stage to deploy changes to the API without impacting the API that is already working
// https://www.terraform.io/docs/providers/aws/r/api_gateway_stage.html
resource "aws_api_gateway_stage" "search-graphql-live" {
  stage_name    = "live"
  rest_api_id   = aws_api_gateway_rest_api.search-api.id
  deployment_id = aws_api_gateway_deployment.deployment.id

  variables = {
    "application_version" = var.application_version
  }

  depends_on = [
    aws_api_gateway_deployment.deployment
  ]
}

// And finally we "publish" the API under the kahula API
// This resource will create a path based API under the specific environment API
// https://www.terraform.io/docs/providers/aws/r/api_gateway_base_path_mapping.html
resource "aws_api_gateway_base_path_mapping" "mapping" {
  api_id     = aws_api_gateway_rest_api.search-api.id
  stage_name = aws_api_gateway_stage.search-graphql-live.stage_name
 
  base_path = "graphql"
  domain_name = terraform.workspace == "prod" ? data.terraform_remote_state.account_resources.outputs.api_gateway_domain.domain_name : data.terraform_remote_state.environment_resources.outputs.api_gateway_domain[0].domain_name

  depends_on = [
    aws_api_gateway_stage.search-graphql-live,
    aws_api_gateway_integration.search_api_integration
  ]
}

# ========================================
# Lambda
# ========================================
resource "aws_lambda_function" "search-graphql" {
  // Deploys the lambda itself https://www.terraform.io/docs/providers/aws/r/lambda_function.html
  // This deployment is generic, despite of being triggered by the API Gateway or EventBridge,
  //  the trigger is configured at the specific session below
  function_name = "${var.application_name}-${terraform.workspace}"
  handler       = "com.tamer84.tango.search.EventHandler"
  filename      = local.code
  runtime       = "java11"
  memory_size = contains(["prod"], terraform.workspace) ? 2048 : 1024
  //This will create a new version of each deployment, allow simple rollback or AB testing
  publish = true
  // This is what triggers the redeploy, if the code has changed a new lambda version is created
  source_code_hash = filebase64sha256(local.code)
  role             = data.terraform_remote_state.account_resources.outputs.lambda_default_exec_role.arn
  timeout          = 300

  environment {
    // This is how you inject environment variables
    variables = {
      VERSION              = var.application_version
      ENVIRONMENT          = terraform.workspace
      APPLICATION_NAME     = "${var.application_name}-${terraform.workspace}"
      ELASTICSEARCH_URL    = "http://${data.terraform_remote_state.environment_resources.outputs.tango_es.endpoint}"
    }
  }

  dead_letter_config {
    target_arn = aws_sqs_queue.dlq.arn
  }

  vpc_config {
    security_group_ids = [data.terraform_remote_state.environment_resources.outputs.group_internal_access.id]
    subnet_ids         = data.terraform_remote_state.environment_resources.outputs.private-subnet.*.id
  }

  tags = {
    Terraform   = "true"
    Environment = terraform.workspace
    Product     = "Tango"
    Name        = "${var.application_name}-${terraform.workspace}"
    Application = var.application_name
    Role        = "search"
    Public      = "true"
    Core        = "false"
  }
}

resource "aws_sqs_queue" "dlq" {
  name                        = "${var.application_name}-${terraform.workspace}"
  fifo_queue                  = false
  content_based_deduplication = false
  message_retention_seconds   = 1209600
}

resource "aws_lambda_provisioned_concurrency_config" "provisioned_concurrency" {
  count                             = contains(["int", "prod"], terraform.workspace) ? 1 : 0
  function_name                     = aws_lambda_function.search-graphql.function_name
  provisioned_concurrent_executions = 10
  qualifier                         = aws_lambda_function.search-graphql.version
}

# ========================================
# CICD
# ========================================
module "cicd" {
  source = "git::ssh://git@github.com/tamer84/infra.git//modules/cicd?ref=develop"

  codestar_connection_arn = data.terraform_remote_state.account_resources.outputs.git_codestar_conn.arn

  pipeline_base_configs = {
    "name"        = "${var.application_name}-${terraform.workspace}"
    "bucket_name" = data.terraform_remote_state.environment_resources.outputs.cicd_bucket.id
    "role_arn"    = data.terraform_remote_state.account_resources.outputs.cicd_role.arn
  }

  codebuild_build_stage = {
    "project_name"        = "${var.application_name}-${terraform.workspace}"
    "github_branch"       = local.cicd_branch
    "github_repo"         = "tamer84/${var.application_name}"
    "github_access_token" = data.terraform_remote_state.account_resources.outputs.github_access_token
    "github_certificate"  = "${data.terraform_remote_state.environment_resources.outputs.cicd_bucket.arn}/${data.terraform_remote_state.environment_resources.outputs.github_cert.id}"

    "service_role_arn"   = data.terraform_remote_state.account_resources.outputs.cicd_role.arn
    "cicd_bucket_id"     = data.terraform_remote_state.environment_resources.outputs.cicd_bucket.id
    "vpc_id"             = data.terraform_remote_state.environment_resources.outputs.vpc.id
    "subnets_ids"        = data.terraform_remote_state.environment_resources.outputs.private-subnet.*.id
    "security_group_ids" = [data.terraform_remote_state.environment_resources.outputs.group_internal_access.id]

    "docker_img_url"                   = data.terraform_remote_state.terraform_build_image_resources.outputs.ecr_repository.repository_url
    "docker_img_tag"                   = "latest"
    "docker_img_pull_credentials_type" = "SERVICE_ROLE"
    "buildspec"                        = "./buildspec.yml"
    "env_vars" = [
      {
        name  = "ENVIRONMENT"
        value = terraform.workspace
    }]
  }
}
