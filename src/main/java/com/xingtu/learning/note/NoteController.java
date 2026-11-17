package com.xingtu.learning.note;

import com.xingtu.learning.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学习笔记接口
 */
@RestController
@RequestMapping("/api/note")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestParam Long userId,
                                              @RequestParam Long lessonId,
                                              @RequestParam Long sectionId,
                                              @RequestParam String content,
                                              @RequestParam(defaultValue = "0") int isPublic) {
        return Result.ok(noteService.create(userId, lessonId, sectionId, content, isPublic));
    }

    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine(@RequestParam Long userId,
                                                  @RequestParam(required = false) Long lessonId) {
        return Result.ok(noteService.mine(userId, lessonId));
    }

    @GetMapping("/public")
    public Result<List<Map<String, Object>>> publicNotes(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(noteService.publicNotes(limit));
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestParam Long userId,
                               @RequestParam Long noteId,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "0") int isPublic) {
        noteService.update(userId, noteId, content, isPublic);
        return Result.ok();
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long userId, @RequestParam Long noteId) {
        noteService.delete(userId, noteId);
        return Result.ok();
    }
}