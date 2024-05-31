package com.bigPicture.backend.service;

import com.bigPicture.backend.domain.Book;
import com.bigPicture.backend.domain.Page;
import com.bigPicture.backend.domain.User;
import com.bigPicture.backend.exception.ResourceNotFoundException;
import com.bigPicture.backend.payload.request.BookCreateRequest;
import com.bigPicture.backend.payload.response.*;
import com.bigPicture.backend.repository.BookRepository;
import com.bigPicture.backend.repository.UserRepository;
import com.bigPicture.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public void save(UserPrincipal userPrincipal, BookCreateRequest request) {

        //요청토큰에 해당하는 user 를 꺼내옴
        User user = userRepository.findById(userPrincipal.getId()).get();

        //페이지는 value 가 배열이라 List<> 이고 이 안에 JSON (DTO) 이 있으므로 이에 맞게 각 스트림을 변환
        List<Page> pages = request.getPages().stream()
                .map(pageDto -> new Page(pageDto.getImage(), pageDto.getContent(), pageDto.getPageNumber()))
                .collect(Collectors.toList());

        Book book = new Book(user, request.getTitle(), request.getCover(), request.getBookLike(),
                pages);

        // 양방향 연관관계 데이터 일관성 유지
        pages.forEach(page -> page.updateBook(book));

        bookRepository.save(book);
    }

    public BookDetailResponse getBookDetails(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        return BookDetailResponse.of(book);
    }

    public List<BookInfoResponse> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        return BooksResponse.of(books); //응답 데이터를 던져야 함으로 DTO 로 변환
    }


    // 페이지네이션된 책 리스트 가져오기
    public List<BookInfoResponse> getRecentPaginatedBooks(int page, int size) {
        int offset = (page - 1) * size;
        List<Book> books = bookRepository.findAllByOrderByIdDesc()
                .stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
        return BooksResponse.of(books);
    }

    public List<BookInfoResponse> getOldPaginatedBooks(int page, int size) {
        int offset = (page - 1) * size;
        List<Book> books = bookRepository.findAllByOrderByIdAsc()
                .stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
        return BooksResponse.of(books);
    }
    @Transactional
    public boolean deleteBookById(Long bookId, Long userId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            if (book.getUser().getId().equals(userId)) {
                bookRepository.delete(book);
                return true;
            }
        }
        return false;
    }

    public List<BookInfoResponse> searchAndPaginateBooks(String name, int page, int size) {
        int offset = (page - 1) * size;
        List<Book> books = bookRepository.findBooksByNameContainingIgnoreCaseOrderByIdAsc(name);
        List<BookInfoResponse> bookInfoResponses = books.stream()
                .skip(offset)
                .limit(size)
                .map(BookInfoResponse::of)
                .collect(Collectors.toList());
        return bookInfoResponses;
    }
}