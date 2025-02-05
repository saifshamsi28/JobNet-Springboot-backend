package com.saif.JobNet.repositories;

import com.saif.JobNet.model.Job;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Function;

@Repository
public interface JobsRepository extends MongoRepository<Job,String> {
    @Query("{ 'title': { $regex: ?0, $options: 'i' }, " +
            "'minSalary': { $gte: ?1 }, " +
            "'location': { $regex: ?3, $options: 'i' }, " +
            "'company': { $regex: ?4, $options: 'i' } }")
    List<Job> findJobsByFilters(String title, String location, Integer minSalary, String company);
}
//    @Override
//    <S extends Job, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);

