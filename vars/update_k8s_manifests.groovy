#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'sharukh83'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'main'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Replace app image
            sed -i "s|\\(image:\\s*\\).*easyshop-app:.*|\\1sharukh8686/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Replace migration image
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|\\(image:\\s*\\).*easyshop-migration:.*|\\1sharukh8686/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Fix ingress
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host:.*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Check diff
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update images to ${imageTag} [ci skip]"

                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/sharukh83/tws-e-commerce-app_hackathon.git HEAD:${gitBranch}
            fi
        """
    }
}
