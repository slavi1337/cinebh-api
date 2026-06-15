pipeline {
    agent any

    environment {
        EC2_HOST = '18.159.94.138'
    }

    stages {
        stage('Build Backend') {
            steps {
                withCredentials([
                    file(credentialsId: 'cinebh-keystore', variable: 'KEYSTORE_FILE')
                ]) {
                    sh '''
                        mkdir -p src/main/resources
                        cp $KEYSTORE_FILE src/main/resources/cinebh-keystore.p12
                        docker build --no-cache -t cinebh-backend:latest .
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'cinebh-smtp2go-password', variable: 'SMTP2GO_PASSWORD'),
                    string(credentialsId: 'cinebh-google-client-id', variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: 'cinebh-google-client-secret', variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: 'cinebh-jwt-secret', variable: 'JWT_SECRET'),
                    string(credentialsId: 'cinebh-keystore-password', variable: 'KEYSTORE_PASSWORD'),
                    string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')
                ]) {
                    sh '''
                        docker save cinebh-backend:latest | gzip > cinebh-backend.tar.gz
                        scp -i /var/lib/jenkins/.ssh/id_ed25519 -o StrictHostKeyChecking=no \
                            cinebh-backend.tar.gz ec2-user@${EC2_HOST}:/home/ec2-user/

                        git clone --depth 1 -b main https://x-access-token:${GITHUB_TOKEN}@github.com/slavi1337/cinebh-web.git /tmp/cinebh-web
                        scp -i /var/lib/jenkins/.ssh/id_ed25519 /tmp/cinebh-web/nginx.conf ec2-user@${EC2_HOST}:/home/ec2-user/nginx.conf
                        rm -rf /tmp/cinebh-web

                        scp -i /var/lib/jenkins/.ssh/id_ed25519 docker-compose.yml ec2-user@${EC2_HOST}:/home/ec2-user/docker-compose.yml

                        ssh -i /var/lib/jenkins/.ssh/id_ed25519 -o StrictHostKeyChecking=no ec2-user@${EC2_HOST} "
                            docker load < /home/ec2-user/cinebh-backend.tar.gz
                            cd /home/ec2-user
                            export SMTP2GO_PASSWORD='${SMTP2GO_PASSWORD}'
                            export GOOGLE_CLIENT_ID='${GOOGLE_CLIENT_ID}'
                            export GOOGLE_CLIENT_SECRET='${GOOGLE_CLIENT_SECRET}'
                            export JWT_SECRET='${JWT_SECRET}'
                            export KEYSTORE_PASSWORD='${KEYSTORE_PASSWORD}'
                            export COOKIE_DOMAIN=cinebhapp.praksa.abhapp.com
                            export COOKIE_SECURE=true
                            export COOKIE_SAME_SITE=None
                            export CORS_ORIGINS=https://cinebhapp.praksa.abhapp.com
                            export FRONTEND_URL=https://cinebhapp.praksa.abhapp.com
                            export STORAGE_PUBLIC_BASE_URL=https://18.159.94.138:9000/cinebh
                            docker-compose down
                            docker-compose up -d
                            sleep 30
                            docker-compose ps
                            sudo systemctl restart nginx
                        "
                    '''
                }
            }
        }
    }
}
