package name.remal.gradleplugins.testsourcesets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class TestSourceSetContainerTest {

    @Test
    @DisplayName("must not declare any methods")
    void mustNotDeclareAnyMethods() {
        for (val method : TestSourceSetContainer.class.getMethods()) {
            assertNotEquals(TestSourceSetContainer.class, method.getDeclaringClass());
        }
    }

}
