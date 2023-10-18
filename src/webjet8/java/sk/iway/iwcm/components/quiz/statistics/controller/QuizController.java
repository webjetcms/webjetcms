package sk.iway.iwcm.components.quiz.statistics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import sk.iway.iwcm.components.quiz.QuizService;

@Controller
@RequestMapping("/components/quiz")
public class QuizController {

   private QuizService quizService;

   @Autowired
   public QuizController(QuizService quizService) {
      this.quizService = quizService;
   }

   @GetMapping("/{quizId}/showStatistics")
   @PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
   public String showQuizStatisticsPage(@PathVariable int quizId, Model model) {
      model.addAttribute("quiz", quizService.getById(quizId));
      model.addAttribute("submittedQuizesCount", quizService.getSubmittedQuizCount(quizId, null, null));
      return "components/quiz/admin_statistics";
   }

}
