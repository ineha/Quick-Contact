package com.myproject.quickcontact.Controller;

import com.myproject.quickcontact.Entities.Contact;
import com.myproject.quickcontact.Entities.User;
import com.myproject.quickcontact.Helper.Message;
import com.myproject.quickcontact.dao.ContactRepository;
import com.myproject.quickcontact.dao.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;

    /*
    method for adding common data to response
     @ModelAttribute  - this is because it will run every time
    because @ModelAttribute methods in a controller are invoked before @RequestMapping methods
    hence we can use it to add common data for each page

     */
    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String name = principal.getName();

        //get the user using username(email)
        User user = userRepository.getUserByUserName(name);
        model.addAttribute(user);

    }

    //dashboard home
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
        model.addAttribute("title","User Dashboard");

        return "normal/user_dashboard";
    }

    //open add contact handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model){
        model.addAttribute("title","Add Contact");
        model.addAttribute("contact",new Contact());
        return "normal/add_contact_form";
    }
/*
   processing add contact form
    ye url match krna chahoye form k action se kunki jb ye url hit(submit) hoga tb isi url p data mapped h

   @ModelAttribute Contact contact --> add_contact_form.html mein hmne object contact liya tha it means jo values wha s a rhi h wo Contact class
   m same honi chaiye--for ex name same hona chhiye dono jgh, to wo sara data map ho jayega
   A method annotated with @ModelAttribute is executed before @RequestMapping method and their specializations such as @GetMapping.


*/

   /*
   @RequestParam annotation -

@ModelAttribute methods in a controller are invoked before @RequestMapping methods

   @ModelAttribute annotation - we can use it in two places(bracket in handler, above method)
   1 - It also helps is binding the data, from front end to DB

@ModelAttribute on a method argument indicates the argument should be retrieved from the model.
So in this case we expect that we have in the Model Contact object as key and we want to get its value and put it to
 the method argument Contact contact
    */



    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) throws IOException {

       try {
           String name = principal.getName();
           User user = this.userRepository.getUserByUserName(name);

           if (file.isEmpty()) {
               System.out.println("image is empty");
               contact.setImage("contact.png");
           } else {
               //upload the file to folder and update the name to contact
               contact.setImage(file.getOriginalFilename());
               File saveFile = new ClassPathResource("static/image").getFile();
               Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
               Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
               System.out.println("image uploaded");
           }

           user.getContacts().add(contact);
           contact.setUser(user);

           this.userRepository.save(user);
           System.out.println("Added to Database ");
        //message success ......
           session.setAttribute("message",new Message("Successfully Added!!","alert-success"));

       }
       catch (Exception e){
           System.out.println("ERROR "+e.getMessage());

           //message error .....
           session.setAttribute("message",new Message("Something went wrong!!"+e.getMessage(),"alert-danger"));

       }
        return "normal/add_contact_form";
    }

    //show contacts handler
    //per page 5 contacts
    //current page
    @GetMapping("/show_contacts/{page}")
    public String showContact(@PathVariable("page")Integer page, Model model,Principal principal){

        model.addAttribute("title","Quick Contact: View Contacts");
        //contact ki list bhejni h database se utha k
        //sbse phle wo user nikal lo principal k help se aur jb wo user mil jaye tb usk contact k list bhej do
        //this will give email
//
//        String name = principal.getName();
//        User user = this.userRepository.getUserByUserName(name);
//        List<Contact> contacts = user.getContacts();
        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);
        int userId=user.getId();
        //this pageable contains 2 information,
        //1 - current page
        //2 - contact per page
        Pageable pageable = PageRequest.of(page, 7);
        Page<Contact> contacts=this.contactRepository.findContactsByUser(userId,pageable);
        model.addAttribute("contacts",contacts);
        model.addAttribute("currentPage",page);
        model.addAttribute("totalPages",contacts.getTotalPages());
        return "normal/show_contacts";
    }

    /*
    @PathVariable annotation is used to bind method parameter to URI template variable
    in this {CID} is URI template variable---> is variable ne is method k Id ko bind kr diya h

    so suppose we call this url : http://localhost:8080/user/contact/19 then in cID we will have 19

     */


    //showing contactDetails
    @GetMapping("/contact/{cId}")
    public String contactDetails(@PathVariable("cId")Integer cId,Model model,Principal principal){
        Optional<Contact> contactOptional = this.contactRepository.findById(cId);
        Contact contact = contactOptional.get();
        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);
        //this is to check if we are getting the particular contact corresponds to a particular user
        if(user.getId()==contact.getUser().getId()){
            model.addAttribute("contact",contact);
            model.addAttribute("title",contact.getName());
        }
        return "normal/show_contact_details";
    }

    //delete contact
    @GetMapping("/delete/{cId}")
    @Transactional
    public String delete(@PathVariable("cId") Integer cId,Principal principal,HttpSession session){

        Contact contact = this.contactRepository.findById(cId).get();

        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);

        //this is to check if we are getting the particular contact corresponds to a particular user
        if(user.getId()==contact.getUser().getId()){
           user.getContacts().remove(contact);
           this.userRepository.save(user);
            //setting message
            session.setAttribute("message",new Message("Contact deleted successfuly...","success"));
        }

        //redirecting to  first page after deleting
        return "redirect:/user/show_contacts/0";
    }
/*
yha p agar get method use krnge to url hit krn p mil jayega, but post method p button click krk h jana pdega



 */
    //open update form handler
    @PostMapping("/update_contact/{cId}")
            public String update_contact(@PathVariable("cId") Integer cId,Principal principal,Model m,HttpSession session){

      m.addAttribute("title","Quick Contact-Update Contact");
        Contact contact = this.contactRepository.findById(cId).get();
        m.addAttribute("contact",contact);
        String name = principal.getName();
        User user = this.userRepository.getUserByUserName(name);

        //this is to check if we are getting the particular contact corresponds to a particular user


        return "normal/update_contact";
    }
//update contact handler
    @PostMapping("/update_contact")
    public String updateHandler(@ModelAttribute Contact contact,HttpSession session, @RequestParam("profileImage") MultipartFile file,Principal principal){

       try {
           //old contact details
           Contact oldContact = this.contactRepository.findById(contact.getcId()).get();

           //image..
           if(!file.isEmpty()){

               //file work-> rewriting the file
               //delete old photo
               File deleteFile = new ClassPathResource("static/image").getFile();
              File file1 = new File(deleteFile,oldContact.getImage());
              file1.delete();
               //update new photo
               File saveFile = new ClassPathResource("static/image").getFile();
               Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
               Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
               contact.setImage(file.getOriginalFilename());
           }
           else{
               contact.setImage(oldContact.getImage());

           }
           System.out.println(contact.getcId());
           String name = principal.getName();
           User user = this.userRepository.getUserByUserName(name);
           user.getContacts().add(contact);
           contact.setUser(user);

           this.userRepository.save(user);
           System.out.println("updating to Database ");
           session.setAttribute("message",new Message("Contact updated successfuly...","success"));

       }
       catch (Exception e){

       }
        return "redirect:/user/contact/"+contact.getcId();
    }

    //Your profile view handler
    @GetMapping("/profile")
    public String viewProfile(){

        return "normal/profile";
    }

}
