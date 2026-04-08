import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpServletRequest;

@Controller
public class DirectoryController {

    @GetMapping("/directory")
    public String directory(HttpServletRequest request, Model model) {
        Object employees = request.getSession().getAttribute("bamboohr_directory");
        model.addAttribute("employees", employees);
        return "directory";
    }
}
