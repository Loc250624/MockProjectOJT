package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.model.entity.Category;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T09:33:45+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class CourseMapperImpl implements CourseMapper {

    @Override
    public CourseResponseDTO toDto(Course entity) {
        if ( entity == null ) {
            return null;
        }

        CourseResponseDTO courseResponseDTO = new CourseResponseDTO();

        courseResponseDTO.setInstructorId( entityInstructorId( entity ) );
        courseResponseDTO.setCategoryId( entityCategoryId( entity ) );
        courseResponseDTO.setRoadmapId( entityRoadmapId( entity ) );
        courseResponseDTO.setId( entity.getId() );
        courseResponseDTO.setTitle( entity.getTitle() );
        courseResponseDTO.setDescription( entity.getDescription() );
        courseResponseDTO.setThumbnailUrl( entity.getThumbnailUrl() );
        courseResponseDTO.setPrice( entity.getPrice() );
        courseResponseDTO.setStatus( entity.getStatus() );
        courseResponseDTO.setCreatedAt( entity.getCreatedAt() );
        courseResponseDTO.setUpdatedAt( entity.getUpdatedAt() );

        return courseResponseDTO;
    }

    @Override
    public Course toEntity(CourseRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.instructor( courseRequestDTOToUser( dto ) );
        course.category( courseRequestDTOToCategory( dto ) );
        course.roadmap( courseRequestDTOToRoadmap( dto ) );
        course.title( dto.getTitle() );
        course.description( dto.getDescription() );
        course.thumbnailUrl( dto.getThumbnailUrl() );
        course.price( dto.getPrice() );
        course.status( dto.getStatus() );

        return course.build();
    }

    private Integer entityInstructorId(Course course) {
        if ( course == null ) {
            return null;
        }
        User instructor = course.getInstructor();
        if ( instructor == null ) {
            return null;
        }
        Integer id = instructor.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityCategoryId(Course course) {
        if ( course == null ) {
            return null;
        }
        Category category = course.getCategory();
        if ( category == null ) {
            return null;
        }
        Integer id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityRoadmapId(Course course) {
        if ( course == null ) {
            return null;
        }
        Roadmap roadmap = course.getRoadmap();
        if ( roadmap == null ) {
            return null;
        }
        Integer id = roadmap.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User courseRequestDTOToUser(CourseRequestDTO courseRequestDTO) {
        if ( courseRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( courseRequestDTO.getInstructorId() );

        return user.build();
    }

    protected Category courseRequestDTOToCategory(CourseRequestDTO courseRequestDTO) {
        if ( courseRequestDTO == null ) {
            return null;
        }

        Category.CategoryBuilder category = Category.builder();

        category.id( courseRequestDTO.getCategoryId() );

        return category.build();
    }

    protected Roadmap courseRequestDTOToRoadmap(CourseRequestDTO courseRequestDTO) {
        if ( courseRequestDTO == null ) {
            return null;
        }

        Roadmap.RoadmapBuilder roadmap = Roadmap.builder();

        roadmap.id( courseRequestDTO.getRoadmapId() );

        return roadmap.build();
    }
}
