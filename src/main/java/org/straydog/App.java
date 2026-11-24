package org.straydog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.staticfiles.Location;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    static final String BASE_URL = Db.env("BASE_URL", "http://localhost:7070");

    public static void main(String[] args) throws Exception {
        Db.init();
        if (Dao.passwordHash("admin") == null) {
            String pw = Db.env("ADMIN_PASS", "admin123");
            Dao.createUser("admin", BCrypt.hashpw(pw, BCrypt.gensalt()), "ADMIN");
            System.out.println("Created user 'admin'. Change the password before real use!");
        }

        Javalin app = Javalin.create(cfg -> cfg.staticFiles.add("/public", Location.CLASSPATH));

        // ---------- PUBLIC: page a QR scan opens (static shell, loads data by JS) ----------
        app.get("/d/{id}", ctx -> ctx.html(page("dog.html")));

        // public JSON (no phone number!) and photo
        app.get("/public/dogs/{id}", ctx -> {
            Dao.Dog d = Dao.findDog(ctx.pathParam("id").toUpperCase());
            if (d == null) { ctx.status(404).result("Not found"); return; }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dogId", d.dogId()); m.put("name", d.name()); m.put("sex", d.sex());
            m.put("ageMonths", d.ageMonths()); m.put("colorMarkings", d.colorMarkings());
            m.put("area", d.area()); m.put("sterilized", d.sterilized()); m.put("sterilizedOn", d.sterilizedOn());
            m.put("hasPhoto", photo(d.dogId()) != null);
            m.put("vaccinations", Dao.vaccinations(d.dogId()));
            ctx.json(m);
        });
        app.get("/photos/{id}", ctx -> {
            String id = ctx.pathParam("id").toUpperCase();
            Path p = id.matches("[A-Z0-9]{6}") ? photo(id) : null;
            if (p == null) { ctx.status(404); return; }
            String n = p.toString();
            ctx.header("Cache-Control", "no-cache");
            ctx.contentType(n.endsWith("png") ? "image/png" : n.endsWith("webp") ? "image/webp" : "image/jpeg")
               .result(Files.newInputStream(p));
        });

        // registration page: login required
        app.get("/register", ctx -> {
            if (ctx.sessionAttribute("user") == null) { ctx.redirect("/"); return; }
            ctx.html(page("register.html"));
        });

        // ---------- LOGIN ----------
        app.post("/login", ctx -> {
            String u = ctx.formParam("username"), p = ctx.formParam("password");
            String hash = (u == null) ? null : Dao.passwordHash(u);
            if (hash != null && p != null && BCrypt.checkpw(p, hash)) {
                ctx.sessionAttribute("user", u);
                ctx.redirect("/register");
            } else ctx.redirect("/?error=1");
        });
        app.get("/logout", ctx -> { ctx.req().getSession().invalidate(); ctx.redirect("/register"); });

        // ---------- VET API (login required) ----------
        app.before("/api/*", ctx -> {
            if (ctx.sessionAttribute("user") == null) throw new UnauthorizedResponse();
        });

        app.post("/api/dogs", ctx -> {
            Dao.Dog d = ctx.bodyAsClass(Dao.Dog.class);
            String id = Dao.createDog(d);
            ctx.status(201).json(Map.of("dogId", id, "publicUrl", BASE_URL + "/d/" + id));
        });

        app.get("/api/dogs/{id}", ctx -> {
            Dao.Dog d = Dao.findDog(ctx.pathParam("id").toUpperCase());
            if (d == null) { ctx.status(404).result("Not found"); return; }
            ctx.json(Map.of("dog", d, "vaccinations", Dao.vaccinations(d.dogId())));
        });

        app.post("/api/dogs/{id}/vaccinations", ctx -> {
            String id = ctx.pathParam("id").toUpperCase();
            if (Dao.findDog(id) == null) { ctx.status(404).result("Dog not found"); return; }
            Dao.Vax in = ctx.bodyAsClass(Dao.Vax.class);
            Dao.addVax(new Dao.Vax(null, id, in.vaccineType(), in.givenOn(), in.nextDue(),
                    in.batchNo(), ctx.sessionAttribute("user")));   // given_by = logged-in user
            ctx.status(201).result("Saved");
        });

        app.post("/api/dogs/{id}/photo", ctx -> {
            String id = ctx.pathParam("id").toUpperCase();
            if (Dao.findDog(id) == null) { ctx.status(404).result("Dog not found"); return; }
            var f = ctx.uploadedFile("photo");
            if (f == null) { ctx.status(400).result("No photo"); return; }
            String ext = f.extension().toLowerCase().replace(".", "");
            if (ext.equals("jpeg")) ext = "jpg";
            if (!List.of("jpg", "png", "webp").contains(ext) || f.size() > 5_000_000) {
                ctx.status(400).result("Use JPG/PNG/WEBP under 5 MB"); return;
            }
            Files.createDirectories(UPLOADS);
            for (String e : List.of("jpg", "png", "webp")) Files.deleteIfExists(UPLOADS.resolve(id + "." + e));
            Files.copy(f.content(), UPLOADS.resolve(id + "." + ext));
            ctx.result("Saved");
        });

        app.get("/api/dogs/{id}/qr", ctx -> {
            String url = BASE_URL + "/d/" + ctx.pathParam("id").toUpperCase();
            BitMatrix m = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 400, 400,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(m, "PNG", out);
            ctx.contentType("image/png").result(out.toByteArray());
        });

        app.exception(Exception.class, (e, ctx) -> { e.printStackTrace(); ctx.status(500).result("Server error: " + e.getMessage()); });

        app.start(Integer.parseInt(Db.env("PORT", "7070")));
        System.out.println("Running at " + BASE_URL + "  (login page: /)");
    }

    static final Path UPLOADS = Path.of("uploads");

    static Path photo(String id) {
        for (String e : List.of("jpg", "png", "webp")) {
            Path p = UPLOADS.resolve(id + "." + e);
            if (Files.exists(p)) return p;
        }
        return null;
    }

    static String page(String name) throws IOException {
        try (var in = App.class.getResourceAsStream("/private/" + name)) { return new String(in.readAllBytes(), "UTF-8"); }
    }

    static String esc(String s) {
        if (s == null) return "-";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
