package com.theironyard.controllers;

import com.theironyard.entities.*;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;


@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    Server dbui = null;

    @PostConstruct
    public void init() throws SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }

//    @Scheduled
//    public void futureDelete(Callable<PhotoRepository> task) {
//        photos.deleteAll();
//    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        User user = users.findFirstByName(username);

        if (user == null) {
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        }

        else if (!PasswordStorage.verifyPassword(password, user.getPassword())) {
            throw new Exception("Wrong password");
        }

        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public User getUser(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return users.findFirstByName(username);
    }


    @RequestMapping("/upload")
    public Photo upload(
            HttpSession session,
            HttpServletResponse response,
            String receiver,
            MultipartFile photo, int timer
    ) throws Exception {
        String username = (String) session.getAttribute("username");

        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findFirstByName(username);
        User receiverUser = users.findFirstByName(receiver);

        if (receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed.");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("build/resources/main/static"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());
        photoFile.deleteOnExit(); //deletes the file

        Photo p = new Photo();
        //need to set Time
        p.setTimer(timer);
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        photos.save(p);

        response.sendRedirect("/");

        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception { //Session is passed as param
        String username = (String) session.getAttribute("username"); // saves the username from session
        if (username == null) { // if no username then throw Exception
            throw new Exception("Not logged in.");
        }
        User user = users.findFirstByName(username); //returns user object:search users repo for current sessions username
        List<Photo> userPhoto = photos.findByRecipient(user); // retrieves all photos by user
        if(userPhoto != null) { // make sure photos exist
            Photo currentPhoto = photos.findPhotoByRecipient(user); // sets the current photo
            int countdown = currentPhoto.getTimer(); // get the time to set for the delay
            TimeUnit.SECONDS.sleep(countdown); // sets the delay
            photos.delete(userPhoto); // deletes the photo
        }
        return photos.findByRecipient(user); // returns photos by that user
    }
}