package org.ansj.splitWord.analysis;

import org.ansj.domain.Term;
import org.ansj.recognition.AsianPersonRecognition;
import org.ansj.recognition.ForeignPersonRecognition;
import org.ansj.recognition.NumRecognition;
import org.ansj.recognition.UserDefineRecognition;
import org.ansj.splitWord.Analysis;
import org.ansj.util.*;
import org.nlpcn.commons.lang.tire.domain.Forest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认用户自定义词性优先
 *
 * @author ansj
 */
public class UserDefineAnalysis extends Analysis {

    @Override
    protected List<Term> getResult(final Graph graph) {
        return new Merger() {

            @Override
            public List<Term> merger() {
                graph.walkPath();
                // 数字发现
                if (MyStaticValue.isNumRecognition && graph.hasNum) {
                    NumRecognition.recognition(graph.terms);
                }

                // 姓名识别
                if (graph.hasPerson && MyStaticValue.isNameRecognition) {
                    // 亚洲人名识别
                    new AsianPersonRecognition(graph.terms).recognition();
                    graph.walkPathByScore();
                    NameFix.nameAmbiguity(graph.terms);
                    // 外国人名识别
                    new ForeignPersonRecognition(graph.terms).recognition();
                    graph.walkPathByScore();
                }

                // 用户自定义词典的识别
                userDefineRecognition(graph, forests);

                return getResult();
            }

            private void userDefineRecognition(final Graph graph, final List<Forest> forests) {
                new UserDefineRecognition(graph.terms, forests).recognition();
                graph.rmLittlePath();
                graph.walkPathByScore();
            }

            private List<Term> getResult() {
                final List<Term> result = new ArrayList<>();
                final int length = graph.terms.length - 1;
                for (int i = 0; i < length; i++) {
                    if (graph.terms[i] != null) {
                        result.add(graph.terms[i]);
                    }
                }
                setRealName(graph, result);

                FilterModifWord.modifResult(result);
                return result;
            }
        }.merger();
    }

    public UserDefineAnalysis(final BufferedReader reader, final List<Forest> forests) {
        super(forests);
        if (reader != null) {
            super.resetContent(new AnsjReader(reader));
        }
    }

    /**
     * 用户自己定义的词典
     *
     * @param forests forests
     */
    public UserDefineAnalysis(final List<Forest> forests) {
        this(null, forests);
    }

    public static List<Term> parse(final String str) {
        return parse(str, null);
    }

    public static List<Term> parse(final String str, final List<Forest> forests) {
        return new UserDefineAnalysis(forests).parseStr(str);
    }
}
