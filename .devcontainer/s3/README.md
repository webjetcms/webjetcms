# Local S3 storage

SeaweedFS is an Apache-2.0 licensed S3-compatible server. This development configuration pins version 4.55, creates the `webjet-autotest` bucket and exposes only `127.0.0.1:9000`. The MinIO Java SDK is an independent Apache-2.0 client dependency; the MinIO server is not required.

```sh
docker compose -f .devcontainer/s3/docker-compose-s3.yml up -d --wait
./gradlew s3IntegrationTest
```

The default development credentials are `webjet-local` / `webjet-local-secret`. Override `S3_ACCESS_KEY`, `S3_SECRET_KEY` and `S3_BUCKET` for both Compose and the test process. Tests also accept `S3_ENDPOINT` (default `http://localhost:9000`). Use a dedicated test bucket: tests create and delete unique `autotest-s3-*` prefixes.

WebJET configuration and migration instructions: [S3 storage](../../docs/sk/install/s3-storage.md).

## VS Code debugger

Select **Debug Docker S3** in Run and Debug, then press F5. Docker must be running and the local database in `poolman-local.xml` must be available. The profile starts SeaweedFS, waits for its health check and starts WebJET with S3 enabled using the development credentials above. It uses the standard HTTP/HTTPS ports 80/443 and debugger port 5005.

The S3 settings are passed only to the launched process through `webjet_*` environment variables; they are not saved to the CMS configuration table. With a new bucket, initialize the base folders as described in the [S3 storage guide](../../docs/sk/install/s3-storage.md#prázdny-bucket). Existing local files are not copied automatically.

Stopping the debugger stops WebJET. SeaweedFS stays running, matching the Docker DB profiles. Use **Tasks: Run Task → Docker S3 Stop** to stop the container without deleting its data. **Docker S3 Start** can also be run independently. The VS Code tasks use the fixed development credentials; if you change them, update both the Docker task and the `webjet_s3*` variables in `appStartDockerS3`.

Stop the server while preserving its data:

```sh
docker compose -f .devcontainer/s3/docker-compose-s3.yml down
```

The named volume retains objects between runs. This single-node setup is for development; it does not configure replication, TLS, backups or production access policies.

Sources: [SeaweedFS](https://github.com/seaweedfs/seaweedfs), [MinIO Java SDK](https://github.com/minio/minio-java).
