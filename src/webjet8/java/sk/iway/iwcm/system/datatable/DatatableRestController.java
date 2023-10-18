package sk.iway.iwcm.system.datatable;
import java.io.Serializable;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 12:50
 *
 *  Abstraktny univerzalny RestController na pracu s DataTables Editor-om
 *
 */
public abstract class DatatableRestController<T, ID extends Serializable>
{
	 private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	 private Validator validator = factory.getValidator();

	 private JpaRepository<T, Long> repo;

	 public DatatableRestController(JpaRepository<T, Long> repo)
	 {
		  this.repo = repo;
	 }

	 @PersistenceUnit
	 private EntityManagerFactory entityManagerFactory;

	 @GetMapping("/all")
	 private Page<T> getAll(Pageable pageable)
	 {

	 	 return repo.findAll(pageable);
	 }

	 @GetMapping("/{id}")
	 private T getOne(@PathVariable("id") long id) {
		  if (repo.existsById(id)) {
				return repo.findById(id).get();
		  } else {
				return null;
		  }
	 }

	 @PostMapping("/add")
	 public ResponseEntity<T> add(@Valid @RequestBody T entity) {

		  //validacia
		  Set<ConstraintViolation<T>> violations = validator.validate(entity);
		  if (!violations.isEmpty()) {
				throw new ConstraintViolationException("Invalid data", violations);
		  } else
		  {
				T newT = repo.save(entity);
				HttpHeaders headers = new HttpHeaders();
				return new ResponseEntity<>(newT, headers, HttpStatus.CREATED);
		  }
	 }

	 @GetMapping("/edit/{id}")
	 public ResponseEntity<T> edit(@PathVariable("id") long id, @Valid @RequestBody T entity)
	 {
		  //validacia
		  Set<ConstraintViolation<T>> violations = validator.validate(entity);
		  if (!violations.isEmpty()) {
				throw new ConstraintViolationException("Invalid data", violations);
		  } else
		  {
				EntityManager em = entityManagerFactory.createEntityManager();
				em.getTransaction().begin();
				T modified = em.merge(entity);
				em.getTransaction().commit();
				em.close();
				return new ResponseEntity<>(modified, null, HttpStatus.OK);
		  }
	 }

	 @SuppressWarnings("rawtypes")
	 @DeleteMapping("/{id}")
	 private ResponseEntity delete(@PathVariable("id") long id) {
		  repo.deleteById(id);
		  return new ResponseEntity(HttpStatus.NO_CONTENT);
	 }

	 /**
	  * This is the method which handles writes from DataTable.Editor. Not really RESTful.
	  */
	 @PostMapping(value = "/editor")
	 public ResponseEntity<DatatableResponse<T>> handleEditor(@RequestBody DatatableRequest<Long, T> request) {
		  DatatableResponse<T> response = new DatatableResponse<>();

		  for (Long id : request.getData().keySet()) {
				if (request.isInsert()) {
					 ResponseEntity<T> re = add(request.getData().get(id));
					 response.add(re.getBody());
				} else if (request.isUpdate()) {
					 ResponseEntity<T> re = edit(id, request.getData().get(id));
					 response.add(re.getBody());
				} else if (request.isDelete()) {
					 delete(id);
				}
		  }

		  return new ResponseEntity<>(response, null, HttpStatus.OK);
	 }
}