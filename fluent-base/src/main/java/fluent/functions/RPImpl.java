/*
 *
 *  Copyright (C) 2021, xyzsd (Zach Del)
 *
 *  Licensed under either of:
 *
 *    Apache License, Version 2.0
 *       (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 *    MIT license
 *       (see LICENSE-MIT) or http://opensource.org/licenses/MIT)
 *
 *  at your option.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *
 */

package fluent.functions;

import fluent.types.FluentValue;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/// Resolved Parameters implementations (package private)
@NullMarked
interface RPImpl {

    // No arguments, just options
    record RP0(Options options) implements ResolvedParameters {

        public RP0 {
            requireNonNull(options);
        }

        @Override
        public boolean hasPositionals() {
            return false;
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            return List.of();
        }

        @Override
        public List<FluentValue<?>> firstPositional() {
            return List.of();
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return Stream.empty();
        }

        @Override
        public int positionalCount() {
            return 0;
        }


    }

    // 1 argument, 1 value (most common)
    // tricky to special case; may eliminate
    record RP11(FluentValue<?> value, Options options) implements ResolvedParameters {
        public RP11 {
            requireNonNull(value);
            requireNonNull(options);
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            if(index == 0) {
                return List.of( value );
            } else {
                return List.of();
            }
        }

        @Override
        public boolean hasPositionals() {
            return true;
        }

        @Override
        public List<FluentValue<?>> firstPositional() {
            return List.of(value);
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return Stream.of(value);
        }

        @Override
        public int positionalCount() {
            return 1;
        }

        @Override
        public boolean isSingle() {
            return true;
        }

        @Override
        public FluentValue<?> singleValue() throws NoSuchElementException {
            return value;
        }
    }

    // 1 argument, many values (2nd most common)
    record RP1n(List<FluentValue<?>> list, Options options) implements ResolvedParameters {
        public RP1n {
            requireNonNull(list);
            requireNonNull(options);
            assert (!list.isEmpty());
            list = List.copyOf(list);
        }

        @Override
        public boolean hasPositionals() {
            return true;
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            if(index == 0) {
                return list;
            } else {
                return List.of();
            }        }

        @Override
        public List<FluentValue<?>> firstPositional() {
            return list;
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return list.stream();
        }

        @Override
        public int positionalCount() {
            return 1;
        }
    }


    // N arguments, each of which can have n (n>0) values
    record RPNn(List<List<FluentValue<?>>> list, Options options) implements ResolvedParameters {

        public RPNn {
            requireNonNull(list);
            requireNonNull(options);
            list = List.copyOf(list);
            assert !list.isEmpty();
            // todo: should assert that lists within the list are also not empty
            //       however ResolvedParameters.from()/of() should take care of that
        }

        @Override
        public boolean hasPositionals() {
            return true;
        }

        @Override
        public List<FluentValue<?>> positional(int index) {
            if(index >= 0 && index < list.size()) {
                return list.get(index);
            } else {
                return List.of();
            }
        }

        @Override
        public List<FluentValue<?>> firstPositional() {
            return list.getFirst();
        }

        @Override
        public Stream<FluentValue<?>> positionals() {
            return list.stream().flatMap(List::stream);
        }

        @Override
        public int positionalCount() {
            return list.size();
        }

    }



}
