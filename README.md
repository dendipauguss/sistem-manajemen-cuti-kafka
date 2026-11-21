# Sistem Manajemen Cuti

Backend ini terdiri dari dua aplikasi Spring Boot yang bertukar event melalui Kafka:

1. `layanan-pengajuan-cuti` menerima dan menyimpan pengajuan, lalu menerbitkan event `leave-requests`.
2. `layanan-persetujuan-cuti` menyimpan pengajuan dari Kafka dan menyediakan API petugas untuk melihat serta memutuskan pengajuan.
3. Keputusan `APPROVED` atau `REJECTED` diterbitkan pada event `leave-decisions` dan memperbarui status di layanan pengajuan.

## Prasyarat

- Java 21
- Maven (atau Maven Wrapper jika tersedia di masing-masing proyek)
- Docker Compose

## Menjalankan aplikasi secara lokal

Ikuti tahap-tahap berikut secara berurutan. Jangan jalankan backend sebelum tahap konfigurasi secret dan akun HR selesai.

### 1. Nyalakan Kafka dan pastikan PostgreSQL siap

Pilih **satu** cara berikut, sesuai kondisi PostgreSQL Anda. Jalankan perintah dari **root proyek**, yaitu `~/starlink-project/sistem-manajemen-cuti`.

Jika belum ada PostgreSQL lokal yang dipakai dan port host `5433` kosong, jalankan PostgreSQL dan Kafka dari Compose:

```bash
cd ~/starlink-project/sistem-manajemen-cuti
docker compose up -d
```

Compose menyiapkan PostgreSQL dan Kafka. Backend secara default terhubung ke `localhost:5432`; karena Compose memetakan PostgreSQL ke port host `5433`, gunakan pengaturan `DB_URL` pada tahap 3 dan 4.

Jika PostgreSQL lokal Anda sudah berjalan di port `5432` dan kedua database proyek tersedia di sana, biarkan PostgreSQL lokal tetap hidup dan jalankan **Kafka saja** (jangan jalankan service PostgreSQL Compose):

```bash
cd ~/starlink-project/sistem-manajemen-cuti
docker compose up -d kafka
```

Pastikan database `layanan_pengajuan_cuti` dan `layanan_persetujuan_cuti` tersedia sebelum melanjutkan. Jangan hentikan PostgreSQL yang sedang dipakai aplikasi lain. Jika memakai PostgreSQL Compose, konfigurasi port host di [docker-compose.yml](./docker-compose.yml) adalah `5433`; pastikan port itu kosong.

### 2. Buat secret JWT

Buat file konfigurasi lokal dari **root proyek**, yaitu `~/starlink-project/sistem-manajemen-cuti`:

```bash
cd ~/starlink-project/sistem-manajemen-cuti
cp .env.example .env
openssl rand -base64 32
nano .env
```

Salin hasil `openssl` ke nilai `JWT_SECRET_BASE64` di file `.env`, hapus teks placeholder, lalu simpan file. Gunakan format `KEY=value` tanpa `export` dan tanpa tanda kutip. File ini dibaca bersama oleh kedua backend. Jangan commit atau membagikan file `.env`; aturan [.gitignore](./.gitignore) mengecualikannya dari Git.

### 3. Konfigurasikan dan jalankan layanan pengajuan (port 8081)

Buat file environment khusus layanan pengajuan dari **root proyek**. Isinya memuat bootstrap HR dan dapat memuat URL database khusus layanan ini:

```bash
cd ~/starlink-project/sistem-manajemen-cuti
cp layanan-pengajuan-cuti/.env.example layanan-pengajuan-cuti/.env
nano layanan-pengajuan-cuti/.env
```

Di file `layanan-pengajuan-cuti/.env`, ganti kata sandi contoh dengan kata sandi kuat (12-72 byte UTF-8). Gunakan format `KEY=value` tanpa `export` dan tanda kutip. Jika menggunakan PostgreSQL dari Compose pada port host `5433`, hapus tanda `#` di baris `DB_URL`. File lokal ini juga dikecualikan Git.

Setelah menyimpan file, buka **terminal pertama** dan jalankan layanan dari direktori `~/starlink-project/sistem-manajemen-cuti/layanan-pengajuan-cuti`:

```bash
cd ~/starlink-project/sistem-manajemen-cuti/layanan-pengajuan-cuti
./mvnw spring-boot:run
```

Jika Maven Wrapper (`./mvnw`) tidak tersedia, gunakan `mvn spring-boot:run` dari direktori yang sama. Biarkan proses berjalan. Saat pertama kali berjalan, layanan membuat akun HR jika email belum terdaftar. Jika akun HR dengan email itu sudah ada, konfigurasi bootstrap tidak mereset kata sandinya; akun dan hash kata sandi disimpan di database `layanan_pengajuan_cuti`.

### 4. Konfigurasikan dan jalankan layanan persetujuan (port 8082)

Jika menggunakan PostgreSQL Compose pada port `5433`, buat dan edit file environment khusus layanan persetujuan dari **root proyek**:

```bash
cd ~/starlink-project/sistem-manajemen-cuti
cp layanan-persetujuan-cuti/.env.example layanan-persetujuan-cuti/.env
nano layanan-persetujuan-cuti/.env
```

Hapus tanda `#` pada `DB_URL` jika memakai PostgreSQL Compose. Gunakan format `KEY=value` tanpa tanda kutip. Jika memakai PostgreSQL di port `5432`, file `.env` khusus ini tidak diperlukan. `JWT_SECRET_BASE64` tidak perlu disalin ke file tersebut: layanan akan membacanya dari `.env` di root proyek yang dibuat pada tahap 2.

Buka **terminal kedua** dan jalankan layanan dari direktori `~/starlink-project/sistem-manajemen-cuti/layanan-persetujuan-cuti`:

```bash
cd ~/starlink-project/sistem-manajemen-cuti/layanan-persetujuan-cuti
./mvnw spring-boot:run
```

Jika Maven Wrapper tidak tersedia, gunakan `mvn spring-boot:run` dari direktori layanan persetujuan yang sama. Biarkan proses berjalan di terminal kedua.

### 5. Jalankan frontend React (port 5173)

Setelah kedua backend berjalan, jalankan dari **terminal ketiga**, di direktori `~/starlink-project/sistem-manajemen-cuti/frontend`:

```bash
cd ~/starlink-project/sistem-manajemen-cuti/frontend
npm install
npm run dev
```

Buka URL yang ditampilkan Vite, biasanya `http://localhost:5173`, lalu masuk menggunakan email dan kata sandi HR dari tahap 3. HR dapat membuat akun pemohon atau petugas dari menu **Kelola akun**. Berikan kata sandi sementara kepada pengguna secara pribadi; pengguna dapat menggantinya melalui menu **Ubah kata sandi**.

Untuk login dan akses API yang terlindungi, backend memerlukan access token Bearer. Token berlaku selama satu jam dan disimpan selama sesi tab browser. Frontend development meneruskan autentikasi serta endpoint pengajuan ke port `8081`, dan endpoint persetujuan ke port `8082`.

### Jika login mendapat HTTP 401 atau akun HR tidak ditemukan

- `401` saat memanggil endpoint API terlindungi tanpa token adalah normal. Login dilakukan lewat `POST /api/auth/login`; endpoint login tidak memerlukan token.
- Perintah `export BOOTSTRAP_ADMIN_...` hanya menyiapkan environment pada terminal saat ini, bukan langsung membuat akun. Akun HR dibuat oleh layanan pengajuan saat startup. Jika backend sudah berjalan sebelum variabel diekspor, hentikan **proses backend saja** dengan `Ctrl+C`, ekspor variabel pada terminal itu, lalu jalankan ulang layanan.
- Variabel `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_ADMIN_NAME`, dan `BOOTSTRAP_ADMIN_NIK` dipetakan aplikasi ke properti bootstrap. Pastikan menjalankan versi kode terbaru yang memuat pemetaan tersebut di [application.properties layanan pengajuan](./layanan-pengajuan-cuti/src/main/resources/application.properties).
- Pastikan semua variabel bootstrap di tahap 3 diisi dengan nilai sebenarnya, bukan teks placeholder. Gunakan email dan kata sandi yang sama saat login. Di log startup layanan pengajuan, cari `Akun HR bootstrap dibuat untuk ...`; jika muncul `HR bootstrap belum dikonfigurasi`, variabel bootstrap belum terbaca.
- Akun berada pada tabel `user_accounts` di database `layanan_pengajuan_cuti`, bukan di database layanan persetujuan. Periksa host dan port yang sama dengan `DB_URL` layanan pengajuan. Default-nya `localhost:5432`; jika memakai PostgreSQL Compose, tahap 3 mengatur port `5433`.

Contoh memeriksa akun dari terminal mana pun (ganti user/port jika konfigurasi Anda berbeda; `psql` akan meminta kata sandi database):

```bash
psql -h localhost -p 5432 -U postgres -d layanan_pengajuan_cuti -c "SELECT email, role FROM user_accounts;"
```

Jika tabel tidak ada atau akun tidak tampil, kemungkinan backend terhubung ke database lain atau bootstrap tidak aktif saat startup. Setelah akun ditemukan, coba login di frontend. Jangan mengirimkan kata sandi atau nilai `JWT_SECRET_BASE64` saat meminta bantuan.

## Autentikasi dan peran

Akun memakai email kantor untuk login dan kata sandi yang disimpan dalam bentuk hash BCrypt. Semua endpoint selain `POST /api/auth/login` memerlukan token. Backend memeriksa peran; menyembunyikan navigasi di React bukan kontrol keamanan.

Peran yang tersedia:

- `APPLICANT`: mengajukan cuti dan hanya melihat pengajuan miliknya.
- `OFFICER`: melihat antrean/pengajuan dan memberi keputusan.
- `HR`: mengelola akun dan memiliki akses petugas.

Akun `APPLICANT` hanya dapat melihat pengajuan yang terkait dengan akunnya. Layanan pengajuan menetapkan nama dan NIK dari token login, bukan dari identitas yang dikirim oleh browser. Data pengajuan lama yang dibuat sebelum akun diterapkan belum otomatis terikat ke akun pemohon.

## API

Login:

`POST http://localhost:8081/api/auth/login`

```json
{
  "email": "hr@perusahaan.test",
  "password": "kata-sandi"
}
```

Respons menyediakan `accessToken`. Kirim token pada setiap endpoint terlindungi dengan header `Authorization: Bearer <accessToken>`. Endpoint login saja yang terbuka tanpa token.

### Ajukan cuti

`POST http://localhost:8081/api/pengajuan-cuti`

```json
{
  "name": "Budi Santoso",
  "nik": "1234567890",
  "leaveDate": "2026-08-20",
  "reason": "Keperluan keluarga"
}
```

Respons berisi `id` pengajuan dan status awal `PENDING`.

### Lihat pengajuan

- `GET http://localhost:8081/api/pengajuan-cuti`
- `GET http://localhost:8081/api/pengajuan-cuti/{id}`

### Petugas melihat antrean/pengajuan

- `GET http://localhost:8082/api/persetujuan-cuti` (semua status)
- `GET http://localhost:8082/api/persetujuan-cuti?status=PENDING`
- `GET http://localhost:8082/api/persetujuan-cuti/{requestId}`

### Petugas memberi keputusan

`POST http://localhost:8082/api/persetujuan-cuti/{requestId}/keputusan`

```json
{
  "status": "APPROVED",
  "decisionNote": "Disetujui"
}
```

Ganti status dengan `REJECTED` untuk menolak pengajuan. Pengajuan yang sudah diputuskan tidak dapat diputuskan kembali. Kedua API menerima JSON; tanggal memakai format ISO `yyyy-MM-dd`.

## Frontend React

Frontend React menyediakan login, formulir pengajuan, riwayat pengajuan milik pemohon, antrean petugas, dan pengelolaan akun HR. Pemohon tidak perlu mengingat ID untuk mengecek status; ID tetap tersedia sebagai referensi di detail riwayat. Cara menjalankan frontend dijelaskan pada tahap 5 di bagian **Menjalankan aplikasi secara lokal** di atas. Untuk deployment, siapkan reverse proxy/hosting yang meneruskan path API ke backend; jangan gunakan konfigurasi development sebagai deployment publik.